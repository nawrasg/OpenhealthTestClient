package es.libresoft.openhealth.android.test.client;

import ieee_11073.part_10101.Nomenclature;

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IAgentService;
import es.libresoft.openhealth.android.aidl.IManagerClientCallback;
import es.libresoft.openhealth.android.aidl.IManagerService;
import es.libresoft.openhealth.android.aidl.IState;
import es.libresoft.openhealth.android.aidl.types.IAttribute;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.measures.IAgentMetric;
import es.libresoft.openhealth.android.aidl.types.measures.IMeasure;

public class AgentMeasureView extends Activity {

	private IAgent agent = null;

	private Vector<IAgentMetric> metrics = new Vector<IAgentMetric>();
	private Handler handler = new Handler();

	private void show(String msg) {
		Toast t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

	private TableRow getShowRowSeparator() {
		TableRow tr = new TableRow(this);
		View v = new View(this);
		v.setMinimumHeight(2);
		v.setMinimumWidth(Integer.MAX_VALUE); //to be sure wrap the line
		v.setBackgroundColor(0xFF909090);
		tr.addView(v);
		return tr;
	}

	private TableRow getShowRow(String str) {
		TableRow tr = new TableRow(this);
		TextView tv = new TextView(this);
		tv.setText(str);
		tr.addView(tv);
		return tr;
	}

	private TableRow getShowRow(ArrayList<String> strs) {
		TableRow tr = new TableRow(this);
		TextView tv = null;
		for (String str: strs) {
			tv = new TextView(this);
			tv.setText(str);
			tr.addView(tv);
		}
		return tr;
	}

	private void showRow(TableRow tr) {
		TableLayout tl = (TableLayout)findViewById(R.id.agentmeasureviewtable);
		tl.addView(tr,0);
	}

	private void showRow(ArrayList<TableRow> rows) {
		TableLayout tl = (TableLayout)findViewById(R.id.agentmeasureviewtable);
		tl.removeAllViews(); //only show the last measure
		while (!rows.isEmpty())
			tl.addView(rows.remove(rows.size()-1),0);
	}

	private void showUnknownAgentMetric(IAgentMetric am) {
		String str = null;
		ArrayList<TableRow> rows = new ArrayList<TableRow>();

		//add attributes
		for (IAttribute att: am.getAttributes()) {
			str = "At[" + att.getAttrId() + "]:" + att.getAttr();
			rows.add(getShowRow(str));
		}

		//add measures
		for (IMeasure me: am.getMeasures()) {
			str = "Me[" + me.getMeasureType() + "]:" + me;
			rows.add(getShowRow(str));
		}

		//add stop separator
		rows.add(getShowRowSeparator());

		//add rows to tablelayout in inverse order
		showRow(rows);
		Log.e("AGENTMEASUREVIEW","SHOWED MEASURE");
	}

	private boolean showSimpleAgentMetric(IAgentMetric am) {
		IAttribute typeMeasure = null;
		IAttribute unit = null;
		IMeasure value = null;
		IMeasure timeStamp = null;

		getAgentMetricAttribute(am, Nomenclature.MDC_ATTR_ID_TYPE, typeMeasure);
		getAgentMetricAttribute(am, Nomenclature.MDC_ATTR_UNIT_CODE, unit);

		getAgentMetricMeasure(am, Nomenclature.MDC_ATTR_NU_VAL_OBS_BASIC, value);
		getAgentMetricMeasure(am, Nomenclature.MDC_ATTR_TIME_STAMP_ABS, timeStamp);

		if (typeMeasure == null ||
			unit == null ||
			value == null
		)
			return false;

		showRow(getShowRowSeparator());
		if (timeStamp != null)
			showRow(getShowRow(""+timeStamp));
		ArrayList<String> strs = new ArrayList<String>(2);
		strs.add(0, ""+value);
		strs.add(1, unit.getAttrIdStr());
		showRow(getShowRow(strs));

		return true;
	}

	private Boolean getAgentMetricAttribute(IAgentMetric am, int attrId, IAttribute attribute) {
		for (IAttribute att: am.getAttributes()) {
			if (att.getAttrId() == attrId) {
				attribute = att;
				return true;
			}
		}
		return false;
	}

	private Boolean getAgentMetricMeasure(IAgentMetric am, int measureId, IMeasure measure) {
		for (IMeasure me: am.getMeasures()) {
			if (me.getMeasureType() == measureId) {
				measure = me;
				return true;
			}
		}
		return false;
	}

	private void showAgentMetric(IAgentMetric am) {
		if (showSimpleAgentMetric(am)) return;
		showUnknownAgentMetric(am);
	}

	private Runnable doUpdateAgentMetricGUI = new Runnable(){
		public void run(){
			IAgentMetric am = null;
			while (!metrics.isEmpty()) {
				am = metrics.remove(0);
				showAgentMetric(am);
			}
		}
	};

	private IManagerClientCallback msc = new IManagerClientCallback.Stub() {

		@Override
		public void agentPlugged(IAgent ag) throws RemoteException {}

		@Override
		public void agentUnplugged(IAgent ag) throws RemoteException {}

		@Override
		public void error(IAgent ag, IError error) throws RemoteException {}

		@Override
		public void agentChangeState(IAgent ag, IState state)
				throws RemoteException {}

		@Override
		public void agentNewMeassure(IAgent ag, IAgentMetric metric)
				throws RemoteException {
			if (ag.getId() != agent.getId() || metric == null) return;

			metrics.add(metric);
			handler.post(doUpdateAgentMetricGUI);
		}

	};

	private void registerManagerCallbacks() {
		IManagerService managerService = (IManagerService)
			GlobalStorage.getInstance().get(IManagerService.class.toString());

		if (managerService == null) {
			show("ManagerService is null, cant get manager callbacks");
			return;
		}
		try {
			managerService.registerApplication(msc);
		} catch(RemoteException e) {
			e.printStackTrace();
			return;
		}
	}

	private void unregisterManagerCallbacks() {
		IManagerService managerService = (IManagerService)
			GlobalStorage.getInstance().get(IManagerService.class.toString());

		if (managerService == null) {
			show("ManagerService is null, cant unregister manager callbacks");
			return;
		}
		try {
			managerService.unregisterApplication(msc);
		} catch(RemoteException e) {
			e.printStackTrace();
			return;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.agentmeasureview);

		Bundle extras  = getIntent().getExtras();
		if (extras == null || !extras.containsKey("agent")) {
			show("Not sended agent");
			finish();
			return;
		}
		agent = extras.getParcelable("agent");
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerManagerCallbacks();
	}

	@Override
	protected void onPause() {
		unregisterManagerCallbacks();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.agentmeasureviewmenu, menu);
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
