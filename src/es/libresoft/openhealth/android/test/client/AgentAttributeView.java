/*
Copyright (C) 2011 GSyC/LibreSoft, Universidad Rey Juan Carlos.

Author: Jose Antonio Santos Cadenas <jcaden@libresoft.es>
Author: Santiago Carot-Nemesio <scarot@libresoft.es>

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

import es.libresoft.openhealth.android.aidl.types.IAttribute;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class AgentAttributeView extends Activity {

	private void show(String msg) {
		Toast t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

	private void showAttributes(ArrayList<IAttribute> attrs) {
		TableLayout tl = (TableLayout)findViewById(R.id.agentattributetable);
		TableRow tr = null;
		TextView tvname = null;
		TextView tvvalue = null;
		for (IAttribute attr : attrs) {
			tr = new TableRow(this);
			tvname = new TextView(this);
			tvvalue = new TextView(this);
			tvname.setText(attr.getAttrIdStr());
			tvvalue.setText(attr.getAttr().toString());
			tr.addView(tvname);
			tr.addView(tvvalue);
			tl.addView(tr);
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.agentattributeview);

		ArrayList<IAttribute> attrs;
		Bundle extras  = getIntent().getExtras();
		if (extras == null || !extras.containsKey("attributes")) {
			show("Not sended attributes to be displayed");
			finish();
			return;
		}
		try {
			attrs = extras.getParcelableArrayList("attributes");
			showAttributes(attrs);
		} catch (Exception e) {
			show("Can't get attributes to be displayed");
			e.printStackTrace();
			finish();
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
