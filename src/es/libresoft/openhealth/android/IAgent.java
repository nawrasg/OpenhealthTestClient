package es.libresoft.openhealth.android;

import android.os.Parcel;
import android.os.Parcelable;

public class IAgent implements Parcelable {
	private int id;

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
		id = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
	}

	public IAgent (int id) {
		this.id = id;
	}

	public boolean equals(Object o) {
		if (!(o instanceof IAgent))
			return false;

		IAgent agent = (IAgent) o;
		return this.id == agent.id;
	}
}
