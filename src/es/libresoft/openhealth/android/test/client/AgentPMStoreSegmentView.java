/*
Copyright (C) 2011 GSyC/LibreSoft, Universidad Rey Juan Carlos.

Author: Bartolomé Marín Sánchez <zeed@libresoft.es>

This program is a (FLOS) free libre and open source implementation
of a multiplatform manager device written in java according to the
ISO/IEEE 11073-20601. Manager application is designed to work in
DalvikVM over android platform.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package es.libresoft.openhealth.android.test.client;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IAgentService;
import es.libresoft.openhealth.android.aidl.types.IAttribute;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.objects.IPM_Segment;
import es.libresoft.openhealth.android.aidl.types.objects.IPM_Store;

public class AgentPMStoreSegmentView extends Activity{

	private IAgent agent = null;
	private IPM_Store store = null;
	private IPM_Segment segment = null;
	private IAgentService agentService = null;

	private void show(String msg) {
		Toast t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.agentpmstoresegmentview);

		Bundle extras  = getIntent().getExtras();
		if (extras == null ||
			!extras.containsKey("agent") ||
			!extras.containsKey("pmstore") ||
			!extras.containsKey("pmsegment")
		) {
			show("Not sended argmunets");
			finish();
			return;
		}
		agent = extras.getParcelable("agent");
		store = extras.getParcelable("pmstore");
		segment = extras.getParcelable("pmsegment");
		agentService = (IAgentService)GlobalStorage.getInstance().get(IAgentService.class.toString());
	}

	private void showAttributes() {
		if (agentService == null) {
			show("showAttributes null args");
			return;
		}

		ArrayList<IAttribute> attrs = new ArrayList<IAttribute>();
		try {
			IError err = new IError();
			agentService.getObjectAttrs(store, attrs, err);
			if (err.getErrCode() != 0) {
				show("Cant get PMStoreAttributes err: " + err.getErrMsg());
				Log.e("AgentPMStoreSegment", "Cant get PMStoreAttributes err: " + err.getErrMsg());
				return;
			}
		} catch(RemoteException e) {
			show("Cant get PMStoreAttributes exception: " + e.getMessage());
			Log.e("AgentPMStoreSegment", "Cant get PMStoreAttributes exception: " + e.getMessage());
			e.printStackTrace();
		}

		//TODO: Show attributes in interface
		for (IAttribute attr: attrs)
			Log.e("AgentPMStoreSegment", "Attr:" + attr.getAttrIdStr());
	}

	@Override
	public void onResume() {
		super.onResume();

		showAttributes();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.agentpmstoresegmentviewmenu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()) {
			case R.id.MENU_DISCONNECT:
				IAgentService agentService = (IAgentService)
					GlobalStorage.getInstance().get(IAgentService.class.toString());
				if (agentService != null) {
					try {
						IError error = new IError();
						if (!agentService.disconnect(agent, error)) {
							show("Error disconnecting: " + error.getErrMsg());
						}
					} catch (RemoteException e) {
						show("Can't connect to the remote Service");
					}
				}
				this.finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
