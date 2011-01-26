package es.libresoft.openhealth.android.test.client;

import es.libresoft.openhealth.android.IAgent;
import es.libresoft.openhealth.android.IManagerClientCallback;
import es.libresoft.openhealth.android.IManagerService;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class ManagerTestClient extends Activity {

	private boolean isBound = false;
	private static String serviceName = new String("es.libresoft.openhealth.android.OPENHEALTH_SERVICE");
	private IManagerService managerService;

	private IManagerClientCallback msc = new IManagerClientCallback() {

		@Override
		public void agentConnected(IAgent agent) throws RemoteException {
			System.out.println("TODO: Implement agent connected");
		}

		@Override
		public void agentDisconnected(IAgent agent) throws RemoteException {
			System.out.println("TODO: Implement agent disconnected");
		}

		@Override
		public IBinder asBinder() {
			System.out.println("TODO: Implement agent asBinder");
			return null;
		}

	};

	private ServiceConnection healthConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			managerService = IManagerService.Stub.asInterface(service);

			try {
				managerService.registerApplication(msc);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			System.err.println("TODO: Service disconnected " + name);
		}
	};

	void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(serviceName), healthConnection, Context.BIND_AUTO_CREATE);
		isBound = true;
	}

	void doUnbindService() {
		if (isBound) {
			// Detach our existing connection.
			unbindService(healthConnection);
			isBound = false;
		}
	}

	private void doStartService() {
		Intent intentDroid = new Intent(serviceName);
		startService(intentDroid);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		doStartService();
		doBindService();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}
}