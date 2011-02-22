/*
Copyright (C) 2008-2011 GSyC/LibreSoft, Universidad Rey Juan Carlos.

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

package es.libresoft.openhealth.android.aidl.types.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class IPM_Segment extends IDIMClass implements Parcelable {

	private IPM_Store store;

	public static final Parcelable.Creator<IPM_Segment> CREATOR =
			new Parcelable.Creator<IPM_Segment>() {
		public IPM_Segment createFromParcel(Parcel in) {
			return new IPM_Segment(in);
		}

		public IPM_Segment[] newArray(int size) {
			return new IPM_Segment[size];
		}
	};

	private IPM_Segment (Parcel in) {
		super(in);
		ClassLoader cl = this.getClass().getClassLoader();
		store = in.readParcelable(cl);
	}

	public IPM_Segment(int handle, IPM_Store store) {
		super(handle, store.getAgent());
		this.store = store;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeParcelable(store, 0);
	}

	public IPM_Store getPMStore() {
		return store;
	}
}
