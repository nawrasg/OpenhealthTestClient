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

package es.libresoft.openhealth.android.aidl.types;

import android.os.Parcel;
import android.os.Parcelable;

public class IAttribute implements Parcelable {
	private Parcelable attr;

	public static final Parcelable.Creator<IAttribute> CREATOR =
			new Parcelable.Creator<IAttribute>() {
		public IAttribute createFromParcel(Parcel in) {
			return new IAttribute(in);
		}

		public IAttribute[] newArray(int size) {
			return new IAttribute[size];
		}
	};

	public IAttribute () {

	}

	private IAttribute (Parcel in) {
		in.readParcelable(null);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(attr, 0);
	}

	public void readFromParcel(Parcel in) {
		java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
		attr = in.readParcelable(cl);
	}

	public IAttribute (Parcelable attr) {
		this.attr = attr;
	}

	public boolean equals(Object o) {
		return attr.equals(o);
	}

	public Parcelable getAttr() {
		return attr;
	}

	public void setAttr(Parcelable attr) {
		this.attr = attr;
	}

}