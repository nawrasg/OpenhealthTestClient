package es.libresoft.openhealth.android.test.client;

import java.util.ArrayList;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IAgentService;
import es.libresoft.openhealth.android.aidl.IPMStoreService;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.objects.IPM_Segment;
import es.libresoft.openhealth.android.aidl.types.objects.IPM_Store;

public class AgentPMStoreView extends ExpandableListActivity {

	private IAgent agent = null;
	private IAgentService agentService = null;
	private IPMStoreService pmStoreService = null;

	private void show(String msg) {
		Toast t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

	private boolean getPMStores(ArrayList<IPM_Store> stores) {
		if (stores == null || agent == null || agentService == null) {
			show("getPMStores objects are null");
			return false;
		}

		IError err = new IError();
		try {
			agentService.getPM_Store(agent, stores, err);
		} catch(RemoteException e) {
			show("Cant get PMStore: RemoteException");
			e.printStackTrace();
			return false;
		}
		if (err.getErrCode() != 0)
			show("Cant get PMStore ret: " + err.getErrMsg());

		return err.getErrCode() == 0;
	}

	private boolean getPMSegment(IPM_Store store, ArrayList<IPM_Segment> segments) {
		if (store == null || segments == null || agent == null || pmStoreService == null) {
			show("getPMSegment objects are null");
			return false;
		}

		IError err = new IError();
		try {
			pmStoreService.updatePMStore(store, err);
			if (err.getErrCode() != 0)
				return false;
			pmStoreService.getAllPMSegments(store, segments, err);
		} catch(RemoteException e) {
			show("Cant get PMSegments: RemoteException");
			e.printStackTrace();
		}
		return err.getErrCode() == 0;
	}

	public class pmStoreExpandableListAdapter extends BaseExpandableListAdapter {

		ArrayList<IPM_Store> stores = null;
		ArrayList<ArrayList<IPM_Segment>> segments = null;
		Context context = null;

		public pmStoreExpandableListAdapter(ArrayList<IPM_Store> lstores, ArrayList<ArrayList<IPM_Segment>> llsegments, Context cnt) {
			stores = lstores;
			segments = llsegments;
			context = cnt;
		}
		public Object getChild(int groupPosition, int childPosition) {
			return segments.get(groupPosition).get(childPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public int getChildrenCount(int groupPosition) {
			return segments.get(groupPosition).size();
		}

		public TextView getGenericView() {
			// Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, 64);

			TextView textView = new TextView(AgentPMStoreView.this);
			textView.setLayoutParams(lp);
			// Center the text vertically
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			// Set the text starting position
			textView.setPadding(36, 0, 0, 0);
			return textView;
		}

		private class LaunchAgentPMStoreSegmentOnClickListener implements OnClickListener {
			private IAgent ag = null;
			private IPM_Store str = null;
			private IPM_Segment seg = null;

			LaunchAgentPMStoreSegmentOnClickListener(IAgent a, IPM_Store s, IPM_Segment sg) {
				ag = a;
				str = s;
				seg = sg;
			}

			@Override
			public void onClick(View v) {
				if (ag == null || str == null || seg == null)
					return;

				//Intent intent = new Intent(AgentPMStoreView.this, AgentPMStoreSegmentView.class);
				Intent intent = new Intent(AgentPMStoreView.this, AgentAttributeView.class);
				intent.putExtra("agent", agent);
				intent.putExtra("pmstore", str);
				intent.putExtra("pmsegment", seg);
				intent.putExtra("idim", seg);
				startActivity(intent);
			}
		};

		private class StartPMSegmentTransferOnLongClickListener implements OnLongClickListener {
			private IAgent ag = null;
			private IPM_Store str = null;
			private IPM_Segment seg = null;
			private Context context = null;

			StartPMSegmentTransferOnLongClickListener(IAgent a, IPM_Store s, IPM_Segment sg, Context cnt) {
				ag = a;
				str = s;
				seg = sg;
				context = cnt;
			}

			@Override
			public boolean onLongClick(View arg0) {
				AgentView agentView = (AgentView)GlobalStorage.getInstance().get(AgentView.class.toString());
				if (pmStoreService == null || agentView == null) {
					show("onLongClick null args");
					return false;
				}

				//TODO: Get alertDialog working!!!
/*
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage("Are you sure you want to startPMSegmentTransfer?");
				builder.setCancelable(true);
				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						show("Clicked yes");
					}
				});

				builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

				AlertDialog alert = builder.create();
				alert.show();
*/
				try {
					IError err = new IError();

					Log.e("AgentPMStoreView", "Call to startPMSegmentTransfer");
					pmStoreService.startPMSegmentTransfer(seg, err);
					Log.e("AgentPMStoreView", "Called to startPMSegmentTransfer");

					if (err.getErrCode() != 0) {
						show("Cant startPMSegmentTransfer err: " + err.getErrMsg());
						Log.e("AgentPMStoreView", "Cant startPMSegmentTransfer err: " + err.getErrMsg());
						return false;
					}
				} catch(RemoteException e) {
					show("Cant startPMSegmentTransfer exception: " + e.getMessage());
					Log.e("AgentPMStoreView", "Cant startPMSegmentTransfer exception: " + e.getMessage());
					e.printStackTrace();
				}

				agentView.getTabHost().setCurrentTab(AgentView.AGENT_VIEW_TAB_MEASURE);
				return true;
			}
		}

		public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
				View convertView, ViewGroup parent) {
			TextView textView = getGenericView();
			textView.setText("PMSegment "+((IPM_Segment)getChild(groupPosition, childPosition)).getInstNumber());

			LaunchAgentPMStoreSegmentOnClickListener eventOnClick =
				new LaunchAgentPMStoreSegmentOnClickListener(
						agent,
						(IPM_Store)getGroup(groupPosition),
						(IPM_Segment)getChild(groupPosition, childPosition));
			StartPMSegmentTransferOnLongClickListener eventOnLongClick =
				new StartPMSegmentTransferOnLongClickListener(
						agent,
						(IPM_Store)getGroup(groupPosition),
						(IPM_Segment)getChild(groupPosition, childPosition),
						context);
			textView.setOnClickListener(eventOnClick);
			textView.setOnLongClickListener(eventOnLongClick);

			return textView;
		}

		public Object getGroup(int groupPosition) {
			return stores.get(groupPosition);
		}

		public int getGroupCount() {
			return stores.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
				ViewGroup parent) {
			TextView textView = getGenericView();
			textView.setText("PMStore "+((IPM_Store)getGroup(groupPosition)).getHandle());

			return textView;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		public boolean hasStableIds() {
			return true;
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
		pmStoreService = (IPMStoreService)GlobalStorage.getInstance().get(IPMStoreService.class.toString());
	}

	private void showPMStores() {
		ArrayList<IPM_Store> stores = new ArrayList<IPM_Store>();
		ArrayList<ArrayList<IPM_Segment>> segments = new ArrayList<ArrayList<IPM_Segment>>();
		if (!getPMStores(stores)) return;

		ArrayList<IPM_Segment> seg;
		for (IPM_Store store : stores) {
			seg = new ArrayList<IPM_Segment>();
			if (!getPMSegment(store, seg)) {
				show("Failed getting segments of store " + store.getHandle());
			}
			segments.add(seg);
		}

		ExpandableListAdapter adapter = new pmStoreExpandableListAdapter(stores, segments, this.getApplicationContext());
        setListAdapter(adapter);
        registerForContextMenu(getExpandableListView());
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
			case R.id.MENU_PMSTORE:
				showPMStores();
				break;
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
