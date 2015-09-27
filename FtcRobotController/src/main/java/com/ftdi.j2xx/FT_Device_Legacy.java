package com.ftdi.j2xx;

import android.util.Log;

public class FT_Device_Legacy extends FT_Device {

    public FT_Device_Legacy(String serialNumber, String description, String ipAddress, int port) {
        super(serialNumber, description, ipAddress, port);
    }

    /*
    ** Packet types
    */
    protected final byte[] WRITE_COMMAND = { 85, -86, 0, 0, 0 };
    protected final byte[] READ_COMMAND = { 85, -86, -128, 0, 0 };
    protected final byte[] RECEIVE_SYNC_COMMAND_3 = { 51, -52, 0, 0, 3};
    protected final byte[] RECEIVE_SYNC_COMMAND = { 51, -52, -128, 0, 0};  // must place length at end
    protected final byte[] CONTROLLER_TYPE_LEGACY = { 0, 77, 73};       // Controller type USBLegacyModule

    public int write(byte[] data, int length, boolean wait)
    {

        int rc = 0;

        if (length <= 0) {
            return rc;
        }

        Log.v(mFT_DeviceDescription, "WRITE(): ToPC len=" + length + " (" + bufferToHexString(data, 0, length) + ")");

        // Check for valid packet
        if (data[0] == PACKET_HEADER_0 && data[1] == PACKET_HEADER_1) {
            if (data[2] == PACKET_WRITE_FLAG) {         // Write command

                // Note: if they are writing LEN bytes then the buffer they are giving us is LEN+5 bytes.
                // since the WRITE_COMMAND header is attached.

                // Don't bother waiting for the reply RECEIVE_SYNC_COMMAND from the PC, just send
                // a quick loop back to save sending a packet.
                RECEIVE_SYNC_COMMAND[4] = 0;
                queueUpForReadFromPhone(RECEIVE_SYNC_COMMAND); // Reply, we got your WRITE_COMMAND

                // Write the buffer to the NetworkManager queue to be sent to the PC Simulator
                super.sendPacketToPC(data, 0, length);

                // Check delta time to see if we are too slow in our simulation.
                // Baud rate was 250,000 with real USB port connected to module
                // We are getting deltas of 31ms between each write call
//                mTimeInMilliseconds = SystemClock.uptimeMillis();
//                mDeltaWriteTime = mTimeInMilliseconds - mOldTimeInMilliseconds;
//                mOldTimeInMilliseconds = mTimeInMilliseconds;
//                Log.v("Legacy", "WRITE: Delta Time = " + mDeltaWriteTime);

            // Read Command
            } else if (data[2] == PACKET_READ_FLAG) {   // READ_COMMAND
                if (data[4] == 3) {  // If they are asking for 3 bytes then send them the controller type right away.
                    queueUpForReadFromPhone(RECEIVE_SYNC_COMMAND_3);  // Send receive sync, bytes to follow
                    queueUpForReadFromPhone(CONTROLLER_TYPE_LEGACY);
                } else {
                    RECEIVE_SYNC_COMMAND[4]=data[4];
                    queueUpForReadFromPhone(RECEIVE_SYNC_COMMAND);  // Send receive sync loop back
                    sendPacketToPC(data, 0, length);   // Send the actual message to the PC so it can respond
                }
            }
        }

        rc = length;
        return rc;
    }

}
