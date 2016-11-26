package com.ultracreation.blelib.impl;

public interface BodyTonerCmdFormaterListener {
	
	/**
	 * Interface for the command ready.
	 * @param data
	 * @param macAddress
	 */
	abstract void OnCommandReady(byte[] data, String macAddress);
}
