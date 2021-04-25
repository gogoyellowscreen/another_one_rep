package com.example.qrscanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.client.result.CalendarParsedResult
import kotlinx.android.synthetic.main.activity_q_r_result.*


private const val TYPE_KEY = "type_key"
private const val TEXT_KEY = "text_key"
class QRResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_q_r_result)

        val type = intent.getStringExtra(TYPE_KEY)!!
        val text = intent.getStringExtra(TEXT_KEY)!!

        qrType.text = type
        qrText.text = text

        buttonOne.setOnClickListener {
            copyText()
        }

        buttonTwo.setOnClickListener {
            searchWeb()
        }

        when (type) {
            "EMAIL_ADDRESS" -> openEmailButton()
            "PRODUCT" -> openLinkButton()
            "URI" -> openLinkButton()
            "GEO" -> Unit
            "TEL" -> openTelButton()
            "SMS" -> openSmsButton()
            "CALENDAR" -> if (Build.VERSION.SDK_INT >= 26) openAddEventButton()
        }
    }

    private fun copyText() {
        val text = qrText.text
        val label = "copy text"
        val clipboardManager: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(applicationContext, "Text copied", Toast.LENGTH_SHORT).show()
    }

    private fun searchWeb() {
        val text = qrText.text
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("http://www.google.com/search?q=$text")
        )
        startActivity(browserIntent)
    }

    private fun openLinkButton() {
        buttonThree.text = "OPEN"
        buttonThree.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(qrText.text.toString()))
            startActivity(browserIntent)
        }
        buttonThree.visibility = View.VISIBLE
    }

    private fun openTelButton() {
        showButton(buttonThree, "CALL") {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${qrText.text}")
            startActivity(intent)
        }

        showButton(buttonFour, "SEND SMS") {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.fromParts(
                        "sms",
                        qrText.text.toString(),
                        null
                    )
                )
            )
        }

        showButton(buttonFive, "ADD CONTACT") {
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, qrText.text)
            }
            startActivity(intent)
        }
    }

    private fun openSmsButton() {
        showButton(buttonThree, "SEND") {
            val smsBody = qrText.text.split("\n")
            val number = smsBody[0]
            val msg = smsBody[1]
            val uri = Uri.parse("smsto:$number")
            val it = Intent(Intent.ACTION_SENDTO, uri)
            it.putExtra("sms_body", msg)
            startActivity(it)
        }
    }

    private fun openEmailButton() {
        showButton(buttonThree, "SEND MSG") {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:${qrText.text}")
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openAddEventButton() {
        val calendarResult = QRApp.instance.lastParsedResult as CalendarParsedResult
        showButton(buttonThree, "ADD EVENT") {
            val intent = Intent(Intent.ACTION_INSERT_OR_EDIT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, calendarResult.summary)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendarResult.start.toInstant().toEpochMilli())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendarResult.start.toInstant().toEpochMilli())
                .putExtra(CalendarContract.Events.DESCRIPTION, calendarResult.description)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, calendarResult.location)
            startActivity(intent)
        }
    }

    private fun showButton(button: Button, text: String, onClick: (View) -> Unit) {
        button.text = text
        button.setOnClickListener(onClick)
        button.visibility = View.VISIBLE
    }
}
