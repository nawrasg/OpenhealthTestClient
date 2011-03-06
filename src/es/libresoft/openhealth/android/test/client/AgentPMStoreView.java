package es.libresoft.openhealth.android.test.client;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IAgentService;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.objects.IPM_Store;

public class AgentPMStoreView extends Activity {

	private IAgent agent = null;
	private IAgentService agentService = null;

	private void show(String msg) {
		Toast t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

	private boolean getPMStores(ArrayList<IPM_Store> stores) {
		if (stores == null || agent == null || agentService == null) {
			show("getPMStores objets are null");
			return false;
		}

		IError err = new IError();
		try {
			agentService.getPM_Store(agent, stores, err);
		} catch(RemoteException e) {
			show("Cant get PMStore RemoteException");
			e.printStackTrace();
			return false;
		}
		if (err.getErrCode() != 0)
			show("Cant get PMStore ret: " + err.getErrMsg());

		return err.getErrCode() == 0;
	}

	private void showPMStores() {
		ArrayList<IPM_Store> stores = new ArrayList<IPM_Store>();
		if (!getPMStores(stores)) return;

		TableLayout tl = (TableLayout)findViewById(R.id.agentpmstoretable);
		tl.removeAllViews();
		TableRow tr = null;
		TextView tvname = null;
		TextView tvvalue = null;
		for (IPM_Store store : stores) {
			tr = new TableRow(this);
			tvname = new TextView(this);
			tvvalue = new TextView(this);
			tvname.setText("PMStore");
			tvvalue.setText(""+store.getHandle());
			tr.addView(tvname);
			tr.addView(tvvalue);
			tl.addView(tr);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.agentpmstoreview);

		Bundle extras  = getIntent().getExtras();
		if (extras == null || !extras.containsKey("agent")) {
			show("Not sended agent");
			finish();
			return;
		}
		agent = extras.getParcelable("agent");
		agentService = (IAgentService)GlobalStorage.getInstance().get(IAgentService.class.toString());
	}

	@Override
	public void onResume() {
		super.onResume();

		showPMStores();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.agentpmstoreviewmenu, menu);
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
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
