package es.libresoft.openhealth.android.types;

import android.os.Parcel;
import android.os.Parcelable;

public class IHANDLE implements Parcelable {
	private int handle;

	public int getHandle() {
		return handle;
	}

	public void setHandle(int handle) {
		this.handle = handle;
	}

	public static final Parcelable.Creator<IHANDLE> CREATOR =
			new Parcelable.Creator<IHANDLE>() {
		public IHANDLE createFromParcel(Parcel in) {
			return new IHANDLE(in);
		}

		public IHANDLE[] newArray(int size) {
			return new IHANDLE[size];
		}
	};

	public IHANDLE () {

	}

	private IHANDLE (Parcel in) {
		handle = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(handle);
	}

	public IHANDLE (int handle) {
		this.handle = handle;
	}

	public boolean equals(Object o) {
		if (!(o instanceof IHANDLE))
			return false;

		IHANDLE agent = (IHANDLE) o;
		return this.handle == agent.handle;
	}
}