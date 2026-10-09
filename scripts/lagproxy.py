"""
Adds artificial lag between Minecraft clients and a server, for testing.

Clients connect to --listen, traffic is forwarded to --target with a delay in each direction.
Data stays in order (it's TCP), so jitter makes delays vary but never reorders anything.

    python lagproxy.py --lag 300 --jitter 50 --spike 1000

Usually started for you by test.cmd -Lag.
"""
import argparse
import asyncio
import random
import time


def parse_args():
    parser = argparse.ArgumentParser(description="Delay TCP traffic to simulate a laggy connection.")
    parser.add_argument("--listen", type=int, default=25566, help="port clients connect to (default 25566)")
    parser.add_argument("--target", default="127.0.0.1:25565", help="server to forward to (default 127.0.0.1:25565)")
    parser.add_argument("--lag", type=int, default=200, help="round trip delay in ms, split across both directions (default 200)")
    parser.add_argument("--jitter", type=int, default=0, help="random +/- ms added to each direction's delay (default 0)")
    parser.add_argument("--spike", type=int, default=0, help="freeze all traffic for this many ms every --spike-every seconds (default 0, off)")
    parser.add_argument("--spike-every", type=float, default=10.0, help="seconds between lag spikes (default 10)")
    return parser.parse_args()


ARGS = parse_args()
TARGET_HOST, TARGET_PORT = ARGS.target.rsplit(":", 1)
ONE_WAY = ARGS.lag / 2000.0
JITTER = ARGS.jitter / 1000.0
SPIKE = ARGS.spike / 1000.0
START = time.monotonic()


def spike_end(now):
    """If now is inside a lag spike, when it ends, otherwise None"""
    if SPIKE <= 0:
        return None
    into_cycle = (now - START) % ARGS.spike_every
    if into_cycle < SPIKE:
        return now + (SPIKE - into_cycle)
    return None


async def pipe(reader, writer):
    """Copy reader to writer, delivering each chunk after the delay without reordering"""
    queue = asyncio.Queue()

    async def receive():
        last_due = 0.0
        try:
            while True:
                data = await reader.read(65536)
                if not data:
                    break
                due = time.monotonic() + ONE_WAY + random.uniform(-JITTER, JITTER)
                # Never deliver before earlier data, TCP can't reorder
                last_due = max(due, last_due)
                await queue.put((last_due, data))
        except (ConnectionError, OSError):
            pass
        await queue.put((None, None))

    async def send():
        try:
            while True:
                due, data = await queue.get()
                if data is None:
                    break
                now = time.monotonic()
                if due > now:
                    await asyncio.sleep(due - now)
                end = spike_end(time.monotonic())
                if end is not None:
                    await asyncio.sleep(end - time.monotonic())
                writer.write(data)
                await writer.drain()
        except (ConnectionError, OSError):
            pass
        finally:
            writer.close()

    await asyncio.gather(receive(), send())


async def handle_client(client_reader, client_writer):
    peer = client_writer.get_extra_info("peername")
    try:
        server_reader, server_writer = await asyncio.open_connection(TARGET_HOST, int(TARGET_PORT))
    except OSError as e:
        print(f"Couldn't reach server at {ARGS.target}: {e}")
        client_writer.close()
        return

    print(f"Client {peer} connected")
    await asyncio.gather(
        pipe(client_reader, server_writer),
        pipe(server_reader, client_writer),
    )
    print(f"Client {peer} disconnected")


async def main():
    server = await asyncio.start_server(handle_client, "127.0.0.1", ARGS.listen)
    spike = f", {ARGS.spike} ms freeze every {ARGS.spike_every:g} s" if SPIKE > 0 else ""
    print(f"Lag proxy: 127.0.0.1:{ARGS.listen} -> {ARGS.target}, {ARGS.lag} ms round trip, +/-{ARGS.jitter} ms jitter{spike}")
    async with server:
        await server.serve_forever()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
