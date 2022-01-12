package marcoluglio;

import java.util.concurrent.CompletableFuture;

/**
 * Tries executing a lambda according to the specified number of attempts an intervals
 */
public final class Retry {

	@FunctionalInterface
	public interface Action {
		RetryStatus call();
	}

	@FunctionalInterface
	public interface AsyncAction {
		RetryStatus call(CompletableFuture<RetryStatus> future);
	}

	public final static int immediately = 0;
	public final static int seconds1 = 1000;
	public final static int seconds5 = 5 * seconds1;
	public final static int seconds30 = 30 * seconds1;
	public final static int minutes1 = 60 * seconds1;
	public final static int minutes3 = 3 * minutes1;
	public final static int minutes5 = 5 * minutes1;

	public final static int[] defaultAttemptIntervals = {
		immediately,
		seconds5,
		seconds30,
		minutes1,
		minutes3,
		minutes5
	};

	public final int[] attemptIntervals;
	public Action action;
	public AsyncAction asyncAction;

	/**
	 * Prepares a lambda execution for retry with default number of attempts and intervals
	 * @param action A lambda that must return RetryStatus.SUCCESS if its execution is considered successful, or RetryStatus.FAILURE otherwise.
	 */
	public Retry(Action action) {
		this(defaultAttemptIntervals, action);
	}

	/**
	 * Prepares a lambda execution for retry with a specific number of attempts and intervals
	 * @param attemptIntervals An array containing the intervals for each attempt in milliseconds
	 * @param action A lambda that must return RetryStatus.SUCCESS if its execution is considered successful, or RetryStatus.FAILURE otherwise.
	 */
	public Retry(int[] attemptIntervals, Action action) {
		this.attemptIntervals = attemptIntervals;
		this.action = action;
	}

	/**
	 * Prepares a lambda execution for retry with default number of attempts and intervals
	 * @param asyncAction A lambda that must wait for async code. It receives a future to signal when the async code finished running and it must assign RetryStatus.SUCCESS to the future if its execution is considered successful, or RetryStatus.FAILURE otherwise.
	 */
	public Retry(AsyncAction asyncAction) {
		this(defaultAttemptIntervals, asyncAction);
	}

	/**
	 * Prepares a lambda execution for retry with a specific number of attempts and intervals
	 * @param attemptIntervals An array containing the intervals for each attempt in milliseconds
	 * @param asyncAction A lambda that must wait for async code. It receives a future to signal when the async code finished running and it must assign RetryStatus.SUCCESS to the future if its execution is considered successful, or RetryStatus.FAILURE otherwise.
	 */
	public Retry(int[] attemptIntervals, AsyncAction asyncAction) {
		this.attemptIntervals = attemptIntervals;
		this.asyncAction = asyncAction;
	}

	/**
	 * Executes the lambda until success or the desired number of attempts has been exceeded
	 * @return True if the lambda indicated a successful execution before reaching the end of attempts. False otherwise.
	 */
	public RetryStatus run() {
		return this.run(false);
	}

	/**
	 * Executes the lambda with the specified intervals between attempts.
	 * @param loop If false, will go through the specified number of attempts and intervals only once. If true, will keep looping between them until it succeeds.
	 * @return True if the lambda indicated a successful execution before reaching the end of attempts. Otherwise it will never return!
	 */
	public RetryStatus run(boolean loop) {

		var retryStatus = RetryStatus.FAILURE;

		do {

			for (var attemptInterval : this.attemptIntervals) {

				// TODO use strategy pattern later maybe

				if (this.action != null) {
					retryStatus = this.action.call();
				} else if (this.asyncAction !=  null) {
					var futureRetryStatus = new CompletableFuture<RetryStatus>();
					retryStatus = this.asyncAction.call(futureRetryStatus);
				}

				if (retryStatus == RetryStatus.SUCCESS) {
					break;
				}

				try {
					Thread.sleep(attemptInterval);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				} // TODO do this async and return a future

			}

		} while (loop);

		retryStatus = RetryStatus.EXCEEDED_ATTEMPTS;

		return retryStatus;

	}

}
