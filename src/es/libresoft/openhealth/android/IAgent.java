package es.libresoft.openhealth.android;

import android.os.Parcel;
import android.os.Parcelable;

public class IAgent implements Parcelable {
	private int phdid;
	private String systId;

	public static final Parcelable.Creator<IAgent> CREATOR =
			new Parcelable.Creator<IAgent>() {
	    public IAgent createFromParcel(Parcel in) {
	        return new IAgent(in);
	    }

	    public IAgent[] newArray(int size) {
	        return new IAgent[size];
	    }
	};

	private IAgent (Parcel in) {
		phdid = in.readInt();
		systId = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(phdid);
		dest.writeString(systId);
	}

	public IAgent (int id, String systId, String manufacturer, String modelNumber) {
		this.phdid = id;
		this.systId = systId;
	}

	public boolean equals(Object o) {
		if (!(o instanceof IAgent))
			return false;

		IAgent agent = (IAgent) o;
		if (this.phdid != agent.phdid)
			return false;

		return this.systId.equalsIgnoreCase(agent.systId);
	}
}
