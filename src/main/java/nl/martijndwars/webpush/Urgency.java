package nl.martijndwars.webpush;


/**
 * Web Push Message Urgency header field values
 *
 *  @see <a href="https://tools.ietf.org/html/rfc8030#section-5.3">Push Message Urgency</a>
 */
public enum Urgency {
	VERY_LOW("very-low"),
	LOW("low"),
	NORMAL("normal"),
	HIGH("high");

	private final String headerValue;

	Urgency(String urgency) {
		this.headerValue = urgency;
	}

	public String getHeaderValue() {
		return headerValue;
	}
}
