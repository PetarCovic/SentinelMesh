import { useEffect, useState } from "react";

type ApiRequest<T> = (signal: AbortSignal) => Promise<T>;

type Result<T> = {
  request: ApiRequest<T>;
  revision: number;
  data: T | null;
  error: string | null;
};

export function useApiQuery<T>(request: ApiRequest<T>, pollMs = 0) {
  const [result, setResult] = useState<Result<T> | null>(null);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let timer: number | undefined;

    async function load() {
      try {
        const data = await request(controller.signal);
        if (!controller.signal.aborted) {
          setResult({ request, revision, data, error: null });
        }
      } catch (error: unknown) {
        if (!controller.signal.aborted) {
          const message =
            error instanceof Error && error.message
              ? error.message
              : "The request failed.";

          setResult((previous) => ({
            request,
            revision,
            data: previous?.request === request ? previous.data : null,
            error: message,
          }));
        }
      } finally {
        if (!controller.signal.aborted && pollMs > 0) {
          timer = window.setTimeout(load, pollMs);
        }
      }
    }

    void load();

    return () => {
      controller.abort();
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [request, revision, pollMs]);

  const current = result?.request === request ? result : null;
  const latest = current?.revision === revision ? current : null;

  return {
    data: current?.data ?? null,
    error: latest?.error ?? null,
    loading: latest === null,
    reload: () => setRevision((value) => value + 1),
  };
}