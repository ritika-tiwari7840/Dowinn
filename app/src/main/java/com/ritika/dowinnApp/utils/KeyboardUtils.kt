package com.ritika.dowinnApp.utils

import android.os.Parcel
import android.os.Parcelable

class KeyboardUtils(private val scrollViewId: Int) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(scrollViewId)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<KeyboardUtils> {
        override fun createFromParcel(parcel: Parcel): KeyboardUtils {
            return KeyboardUtils(parcel)
        }

        override fun newArray(size: Int): Array<KeyboardUtils?> {
            return arrayOfNulls(size)
        }
    }

}
