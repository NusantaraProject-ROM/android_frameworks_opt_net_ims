/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ims.rcs.uce.eab;

import static android.telephony.CarrierConfigManager.Ims.KEY_NON_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC_INT;
import static android.telephony.ims.RcsContactUceCapability.REQUEST_RESULT_FOUND;
import static android.telephony.ims.RcsContactUceCapability.REQUEST_RESULT_NOT_FOUND;
import static android.telephony.ims.RcsContactUceCapability.SOURCE_TYPE_NETWORK;

import static com.android.ims.rcs.uce.eab.EabProvider.CONTACT_URI;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.ims.RcsContactPresenceTuple;
import android.telephony.ims.RcsContactUceCapability;
import android.test.mock.MockContentResolver;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.android.ims.ImsTestBase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

@RunWith(AndroidJUnit4.class)
public class EabControllerTest extends ImsTestBase {
    EabProviderTestable mEabProviderTestable = new EabProviderTestable();
    EabControllerImpl mEabController;
    PersistableBundle mBundle;

    private static final int TEST_SUB_ID = 1;
    private static final String TEST_PHONE_NUMBER = "16661234567";
    private static final String TEST_SERVICE_STATUS = "status";
    private static final String TEST_SERVICE_SERVICE_ID = "serviceId";
    private static final String TEST_SERVICE_VERSION = "version";
    private static final String TEST_SERVICE_DESCRIPTION = "description";
    private static final boolean TEST_AUDIO_CAPABLE = true;
    private static final boolean TEST_VIDEO_CAPABLE = false;

    private static final Uri TEST_CONTACT_URI = Uri.parse(TEST_PHONE_NUMBER + "@android.test");

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockContentResolver mockContentResolver =
                (MockContentResolver) mContext.getContentResolver();
        mEabProviderTestable.initializeForTesting(mContext);
        mockContentResolver.addProvider(EabProvider.AUTHORITY, mEabProviderTestable);

        insertContactInfoToDB();
        mEabController = new EabControllerImpl(
                mContext, TEST_SUB_ID, null, Looper.getMainLooper());

        mBundle = mContextFixture.getTestCarrierConfigBundle();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    @SmallTest
    public void testGetAvailability() {
        List<RcsContactUceCapability> contactList = new ArrayList<>();
        contactList.add(createPresenceCapability(false));

        mEabController.saveCapabilities(contactList);

        EabCapabilityResult result = mEabController.getAvailability(TEST_CONTACT_URI);
        Assert.assertEquals(EabCapabilityResult.EAB_QUERY_SUCCESSFUL, result.getStatus());
        Assert.assertEquals(TEST_CONTACT_URI,
                result.getContactCapabilities().getContactUri());
    }

    @Test
    @SmallTest
    public void testGetCapability() {
        List<RcsContactUceCapability> contactList = new ArrayList<>();
        contactList.add(createPresenceCapability(false));

        mEabController.saveCapabilities(contactList);

        List<Uri> contactUriList = new ArrayList<>();
        contactUriList.add(TEST_CONTACT_URI);
        Assert.assertEquals(1,
                mEabController.getCapabilities(contactUriList).size());
        Assert.assertEquals(EabCapabilityResult.EAB_QUERY_SUCCESSFUL,
                mEabController.getCapabilities(contactUriList).get(0).getStatus());
    }

    @Test
    @SmallTest
    public void testGetExpiredCapability() {
        List<RcsContactUceCapability> contactList = new ArrayList<>();
        contactList.add(createPresenceCapability(true));

        mEabController.saveCapabilities(contactList);

        List<Uri> contactUriList = new ArrayList<>();
        contactUriList.add(TEST_CONTACT_URI);
        Assert.assertEquals(1,
                mEabController.getCapabilities(contactUriList).size());
        Assert.assertEquals(EabCapabilityResult.EAB_CONTACT_EXPIRED_FAILURE,
                mEabController.getCapabilities(contactUriList).get(0).getStatus());
    }

    @Test
    @SmallTest
    public void testNonRcsCapability() {
        // Set non-rcs capabilities expiration to 121 days
        mBundle.putInt(KEY_NON_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC_INT, 121 * 24 * 60 * 60);
        // Set timestamp to 120 days age
        GregorianCalendar date = new GregorianCalendar();
        date.setTimeZone(TimeZone.getTimeZone("UTC"));
        date.add(Calendar.DATE, -120);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                .format(date.getTime());

        List<RcsContactUceCapability> contactList = new ArrayList<>();
        contactList.add(createPresenceNonRcsCapability(timestamp));

        mEabController.saveCapabilities(contactList);

        List<Uri> contactUriList = new ArrayList<>();
        contactUriList.add(TEST_CONTACT_URI);

        // Verify result is not expired
        Assert.assertEquals(1,
                mEabController.getCapabilities(contactUriList).size());
        Assert.assertEquals(EabCapabilityResult.EAB_QUERY_SUCCESSFUL,
                mEabController.getCapabilities(contactUriList).get(0).getStatus());
    }

    @Test
    @SmallTest
    public void testNonRcsCapabilityExpired() {
        // Set non-rcs capabilities expiration to 119 days
        mBundle.putInt(KEY_NON_RCS_CAPABILITIES_CACHE_EXPIRATION_SEC_INT, 119 * 24 * 60 * 60);
        // Set timestamp to 120 days age
        GregorianCalendar date = new GregorianCalendar();
        date.setTimeZone(TimeZone.getTimeZone("UTC"));
        date.add(Calendar.DATE, -120);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                .format(date.getTime());

        List<RcsContactUceCapability> contactList = new ArrayList<>();
        contactList.add(createPresenceNonRcsCapability(timestamp));
        mEabController.saveCapabilities(contactList);

        // Verify result is expired
        List<Uri> contactUriList = new ArrayList<>();
        contactUriList.add(TEST_CONTACT_URI);
        Assert.assertEquals(1,
                mEabController.getCapabilities(contactUriList).size());
        Assert.assertEquals(EabCapabilityResult.EAB_CONTACT_EXPIRED_FAILURE,
                mEabController.getCapabilities(contactUriList).get(0).getStatus());
    }

    private RcsContactUceCapability createPresenceCapability(boolean isExpired) {
        String timestamp;
        GregorianCalendar date = new GregorianCalendar();
        date.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (isExpired) {
            date.add(Calendar.DATE, -120);
        } else {
            date.add(Calendar.DATE, 120);
        }

        timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                .format(date.getTime());

        RcsContactPresenceTuple.ServiceCapabilities.Builder serviceCapabilitiesBuilder =
                new RcsContactPresenceTuple.ServiceCapabilities.Builder(TEST_AUDIO_CAPABLE,
                        TEST_VIDEO_CAPABLE);
        RcsContactPresenceTuple tupleWithServiceCapabilities =
                new RcsContactPresenceTuple.Builder(TEST_SERVICE_STATUS, TEST_SERVICE_SERVICE_ID,
                        TEST_SERVICE_VERSION)
                        .setServiceDescription(TEST_SERVICE_DESCRIPTION)
                        .setContactUri(TEST_CONTACT_URI)
                        .setServiceCapabilities(serviceCapabilitiesBuilder.build())
                        .setTimestamp(timestamp)
                        .build();

        RcsContactPresenceTuple tupleWithEmptyServiceCapabilities =
                new RcsContactPresenceTuple.Builder(TEST_SERVICE_STATUS, TEST_SERVICE_SERVICE_ID,
                        TEST_SERVICE_VERSION)
                        .setServiceDescription(TEST_SERVICE_DESCRIPTION)
                        .setContactUri(TEST_CONTACT_URI)
                        .setTimestamp(timestamp)
                        .build();

        RcsContactUceCapability.PresenceBuilder builder =
                new RcsContactUceCapability.PresenceBuilder(
                        TEST_CONTACT_URI, SOURCE_TYPE_NETWORK, REQUEST_RESULT_FOUND);
        builder.addCapabilityTuple(tupleWithServiceCapabilities);
        builder.addCapabilityTuple(tupleWithEmptyServiceCapabilities);
        return builder.build();
    }

    private RcsContactUceCapability createPresenceNonRcsCapability(String timestamp) {
        RcsContactPresenceTuple.ServiceCapabilities.Builder serviceCapabilitiesBuilder =
                new RcsContactPresenceTuple.ServiceCapabilities.Builder(false, false);
        RcsContactPresenceTuple tupleWithServiceCapabilities =
                new RcsContactPresenceTuple.Builder(TEST_SERVICE_STATUS, TEST_SERVICE_SERVICE_ID,
                        TEST_SERVICE_VERSION)
                        .setServiceDescription(TEST_SERVICE_DESCRIPTION)
                        .setContactUri(TEST_CONTACT_URI)
                        .setServiceCapabilities(serviceCapabilitiesBuilder.build())
                        .setTimestamp(timestamp)
                        .build();

        RcsContactUceCapability.PresenceBuilder builder =
                new RcsContactUceCapability.PresenceBuilder(
                        TEST_CONTACT_URI, SOURCE_TYPE_NETWORK, REQUEST_RESULT_NOT_FOUND);
        builder.addCapabilityTuple(tupleWithServiceCapabilities);
        return builder.build();
    }

    private void insertContactInfoToDB() {
        ContentValues data = new ContentValues();
        data.put(EabProvider.ContactColumns.PHONE_NUMBER, TEST_PHONE_NUMBER);
        data.put(EabProvider.ContactColumns.RAW_CONTACT_ID, 1);
        mContext.getContentResolver().insert(CONTACT_URI, data);
    }
}
