package com.ubergeek42.WeechatAndroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import androidx.preference.DialogPreference.TargetFragment
import com.ubergeek42.WeechatAndroid.preferences.Pref
import com.ubergeek42.WeechatAndroid.preferences.Preference.Companion.getByKey
import com.ubergeek42.WeechatAndroid.upload.main
import com.ubergeek42.WeechatAndroid.utils.Toaster.Companion.ErrorToast

class PreferencesActivity : AppCompatActivity(),
                            PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preferences)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // this is exactly the place for the following statement. why? no idea.
        if (savedInstanceState != null) return

        val p = supportFragmentManager.findFragmentByTag(null) ?: PreferencesFragment()

        intent.getStringExtra(KEY)?.let {
            val args = Bundle()
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, it)
            p.arguments = args
        }

        supportFragmentManager.beginTransaction()
                .add(R.id.preferences, p, null)
                .commit()
    }

    override fun onPreferenceStartScreen(preferenceFragmentCompat: PreferenceFragmentCompat,
                                         preferenceScreen: PreferenceScreen): Boolean {
        if (Pref.notificationsGroup.key == preferenceScreen.key && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        } else {
            val intent = Intent(this@PreferencesActivity, PreferencesActivity::class.java)
            intent.putExtra(KEY, preferenceScreen.key)
            startActivity(intent)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    class PreferencesFragment : PreferenceFragmentCompat(), TargetFragment, Preference.OnPreferenceChangeListener {
        private var key: String? = null
        //private var sslGroup: Preference? = null
        //private var sshGroup: Preference? = null
        //private var wsPath: Preference? = null

        private var resumePreference: Preference? = null

        override fun onDisplayPreferenceDialog(preference: Preference?) {

            if (preference is FontPreference || preference is ThemePreference) {
                val granted = ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                    resumePreference = preference
                    return
                }
            }

            when(preference) {
                is DialogFragmentGetter -> {
                    val externalCode = when (preference.key) {
                        Pref.connection.ssh.serializedKey.key -> SSH_KEY
                        Pref.connection.ssl.clientCertificate.key -> CLIENT_CERT
                        else -> -1
                    }

                    val fragment = makeFragment(preference as DialogFragmentGetter, externalCode)
                    fragment.setTargetFragment(this, 0)
                    fragment.show(parentFragmentManager, FRAGMENT_DIALOG_TAG)

                }
                is RingtonePreferenceFix -> startActivityForResult(
                        preference.makeRingtoneRequestIntent(),
                        RINGTONE)
                else -> super.onDisplayPreferenceDialog(preference)
            }
        }

        // don't check permissions if preference is null, instead use resumePreference
        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                                grantResults: IntArray) {
            onDisplayPreferenceDialog(resumePreference)
        }

        // this makes fragment display preferences. key is the key of the preference screen
        // that this fragment is supposed to display. the key is set in activity's onCreate
        override fun onCreatePreferences(bundle: Bundle?, key: String?) {
            this.key = key
            setPreferencesFromResource(R.xml.preferences, key)
            preferenceScreen.fixMultiLineTitles()
            preferenceScreen.forEveryPreference {
                it.onPreferenceChangeListener = this
            }
            enableDisablePreferences()
        }

        // this only sets the title of the action bar
        override fun onActivityCreated(savedInstanceState: Bundle?) {
            val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
            actionBar?.title = if (key == null) {
                getString(R.string.menu__preferences)
            } else {
                findPreference<Preference>(key!!)!!.title
            }
            super.onActivityCreated(savedInstanceState)
        }

        // this is required for RingtonePreferenceFix, which requires an activity to operate
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == RESULT_OK && data != null) {
                when (requestCode) {
                    RINGTONE -> (findPreference<Preference>(Pref.notifications.sound.key)
                            as RingtonePreferenceFix).onActivityResult(data)
                    SSH_KEY -> (findPreference<Preference>(Pref.connection.ssh.serializedKey.key)
                            as FilePreference).onActivityResult(data)
                    CLIENT_CERT -> (findPreference<Preference>(Pref.connection.ssl.clientCertificate.key)
                            as FilePreference).onActivityResult(data)
                }
            }
        }

        override fun onPreferenceChange(preference: Preference, o: Any): Boolean {
            return try {
                Pref.validate(preference.key, o)
                main { enableDisablePreferences() }
                true
            } catch (e: Exception) {
                ErrorToast.show(R.string.error__etc__prefix, e.message)
                false
            }
        }

        private fun makeFragment(preference: DialogFragmentGetter, code: Int): DialogFragment {
            val fragment = preference.dialogFragment
            fragment.arguments = Bundle(1).apply {
                putString(KEY, (preference as Preference).key!!)
                if (code != -1) putInt("code", code)
            }
            return fragment
        }

        private fun enableDisablePreferences() {
            preferenceScreen.forEveryPreference { pref ->
                getByKey(pref.key)?.let {
                    pref.isEnabled = it.notDisabled
                    pref.isVisible = it.notHidden
                }
            }
        }
    }
}

private const val FRAGMENT_DIALOG_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG"


private const val KEY = "key"
private const val RINGTONE = 0
private const val SSH_KEY = 1
private const val CLIENT_CERT = 3

private fun PreferenceGroup.forEveryPreference(lambda: (Preference) -> Unit) {
    for (i in 0 until preferenceCount) {
        val preference = getPreference(i)

        lambda(preference)

        if (preference is PreferenceScreen) continue

        if (preference is PreferenceGroup) {
            preference.forEveryPreference(lambda)
        }
    }
}

private fun PreferenceGroup.fixMultiLineTitles() {
    forEveryPreference {
        it.isSingleLineTitle = false
    }
}
