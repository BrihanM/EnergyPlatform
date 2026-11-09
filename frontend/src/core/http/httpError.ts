/**
 * Represents an HTTP error returned by the server or network.
 * Extends the native Error class with additional HTTP-specific information.
 */
export class HttpError extends Error {
  /** The HTTP status code. */
  readonly status: number
  /** Application-specific error code. */
  readonly code?: string
  /** Correlation ID for tracking across logs. */
  readonly correlationId?: string

  /**
   * Creates a new HttpError instance.
   * 
   * @param {Object} options - Error configuration.
   * @param {number} options.status - The HTTP status code.
   * @param {string} [options.code] - Application-specific error code.
   * @param {string} [options.message] - Human-readable error message.
   * @param {string} [options.correlationId] - Correlation ID for debugging.
   */
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
