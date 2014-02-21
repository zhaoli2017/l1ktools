package org.flowcyt.cfcs;
// Coerce an oversized binary FCS file back to normal (<= 32 bits per parameter) size

// Usage: java OversizeToInteger file:oversizeDestination.fcs file:integerSource.fcs


public class OversizeToInteger {

	private static final int INPUT = 0, OUTPUT = 1;

	private static final int BITSPERBYTE = 8;
	private static final int BYTESPERINTEGER = 4;
	private static final int BYTESPEROVERSIZE = 8;

	static int failIfError(CFCSError error) {
		int errorType = 0;

		while (error != null) {
			System.err.println(error);

			if (error.errorNumber < 0) {
				errorType = -1;
				}
			else if (error.errorNumber > 0 && errorType == 0) {
				errorType = 1;
				}

			error = error.nextError;
			}

		return errorType;
		}

	public static void main(String[] argv) {
		CFCSSystem readSystem = new CFCSSystem();

		try { readSystem.open(argv[INPUT]); }
		catch (CFCSError error) {
			if (failIfError(error) != 0) System.exit(1);
			}

		CFCSSystem writeSystem = new CFCSSystem();

		try { writeSystem.create(argv[OUTPUT]); }
		catch (CFCSError error) {
			if (failIfError(error) != 0) System.exit(1);
			}

		int nDataSets = readSystem.getCount();

		for (int iset = 0; iset < nDataSets; iset++) {
			CFCSDataSet readSet = readSystem.getDataSet(iset);
			CFCSKeywords readKeywords = readSet.getKeywords();
			CFCSParameters readParam = readSet.getParameters();
			CFCSData readData = readSet.getData();
			
			int type = readData.getType();

			CFCSDataSet writeSet = writeSystem.createDataSet(type);
			CFCSKeywords writeKeywords = writeSet.getKeywords();
			CFCSParameters writeParam = writeSet.getParameters();
			CFCSData writeData = writeSet.getData();
				
			for (int i = 0, count = readParam.getCount(); i < count; i++) {
				CFCSParameter parameter = readParam.getParameter(i);
				parameter.setFieldSize(BYTESPERINTEGER * BITSPERBYTE);
				writeParam.addParameter(parameter);
				}

			switch (type) {
				case CFCSData.LISTMODE : /* ListModeData */
					
					int count = readParam.getCount();

					// allocate arrays to hold one event
					int[] event = new int[count];
					byte[][] bytes = new byte[count][BYTESPEROVERSIZE];

					int nEvents = ((CFCSListModeData) readData).getCount();

					for (int idx = 0; idx < nEvents; idx++) {
						((CFCSListModeData) readData).getEvent(idx, BYTESPEROVERSIZE, bytes);
						
						for (int i = 0; i < count; i++) {
							for (int j = 0; j < BYTESPERINTEGER; j++) {
								int data = bytes[i][(BYTESPEROVERSIZE - BYTESPERINTEGER) + j] & 0xff;
								event[i] |= data << (((BYTESPERINTEGER - j) - 1) * BITSPERBYTE);
								}
							}

						((CFCSListModeData) writeData).addEvent(event);
						}
					break;

				default: System.exit(1);
				}

			// Try to copy all the 'safe' keywords that haven't already been set

			for (int i = 0, count = readKeywords.getCount(); i < count; i++) {
				CFCSKeyword keyword = readKeywords.getKeyword(i);

				try { writeKeywords.getKeyword(keyword.getKeywordName()); }
				catch (CFCSError error) {
					if (error.errorNumber == CFCSErrorCodes.CFCSKeywordNotFound) {
						try { writeKeywords.addKeyword(keyword); }
						catch (CFCSError exception) { }
						}
					else if (failIfError(error) != 0) System.exit(1);
					}
				}
			}

		readSystem.close();
		writeSystem.close();
		}
	}
