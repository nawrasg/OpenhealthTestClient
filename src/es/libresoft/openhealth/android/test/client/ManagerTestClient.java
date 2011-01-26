package es.libresoft.openhealth.android.test.client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public class ManagerTestClient extends Activity {

	private static String serviceName = new String("es.libresoft.openhealth.android.OPENHEALTH_SERVICE");
	private ServiceConnection managerService = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			System.err.println("TODO: Service connected " + name);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			System.err.println("TODO: Service disconnected " + name);
		}
	};

	public boolean initManager() {

		bindService(new Intent(serviceName), managerService, Context.BIND_AUTO_CREATE);

		System.err.println("Service launched");
		return true;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		initManager();
	}
}