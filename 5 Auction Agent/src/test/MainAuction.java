package test;

public class MainAuction {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			logist.LogistPlatform.main(args);
		} else {
			String[] defaultArgs = { "config/auction.xml", "test-risk-seeking", "test-best-response"};
			logist.LogistPlatform.main(defaultArgs);
		}
	}
}
