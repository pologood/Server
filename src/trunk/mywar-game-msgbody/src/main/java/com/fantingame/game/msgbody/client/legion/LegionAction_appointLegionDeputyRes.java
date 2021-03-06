package com.fantingame.game.msgbody.client.legion;

import com.fantingame.game.msgbody.common.io.iface.IXInputStream;
import com.fantingame.game.msgbody.common.io.iface.IXOutStream;
import com.fantingame.game.msgbody.common.model.ICodeAble;
import java.io.IOException;


/**任命副军团长**/
public class LegionAction_appointLegionDeputyRes implements ICodeAble {

		/**晋升的用户id**/
	private String deputyUserId="";

	
	@Override
	public void encode(IXOutStream outputStream) throws IOException {
		outputStream.writeUTF(deputyUserId);


	}

	@Override
	public void decode(IXInputStream inputStream) throws IOException {
		deputyUserId = inputStream.readUTF();


	}
	
		/**晋升的用户id**/
    public String getDeputyUserId() {
		return deputyUserId;
	}
	/**晋升的用户id**/
    public void setDeputyUserId(String deputyUserId) {
		this.deputyUserId = deputyUserId;
	}

	
	
}
