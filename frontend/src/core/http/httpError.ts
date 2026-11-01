export class HttpError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly correlationId?: string;

  constructor(options: {
    status: number;
    code?: string;
    message?: string;
    correlationId?: string;
  }) {
    super(options.message ?? `Request failed with status ${options.status}`);
    this.name = 'HttpError';
    this.status = options.status;
    this.code = options.code;
    this.correlationId = options.correlationId;
  }
}
