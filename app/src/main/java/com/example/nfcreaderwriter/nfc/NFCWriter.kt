package com.example.nfcreaderwriter.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.example.nfcreaderwriter.data.models.NfcResult
import java.io.IOException

object NFCWriter {
    fun write(tag: Tag, message: NdefMessage, verify: Boolean): NfcResult<String> {
        val bytesRequired = message.toByteArray().size
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                when {
                    !ndef.isWritable -> NfcResult.Error("This tag is permanently read-only.")
                    // maxSize includes the complete encoded NDEF message, not just visible text.
                    bytesRequired > ndef.maxSize -> NfcResult.Error(
                        "The content needs $bytesRequired bytes, but this tag holds ${ndef.maxSize} bytes."
                    )
                    else -> {
                        ndef.writeNdefMessage(message)
                        if (verify) {
                            // Re-read through the same live connection and compare exact NDEF bytes.
                            val written = ndef.ndefMessage
                            if (written?.toByteArray()?.contentEquals(message.toByteArray()) == true) {
                                NfcResult.Success("Write verified successfully")
                            } else {
                                NfcResult.Error("The write completed, but verification did not match.")
                            }
                        } else {
                            NfcResult.Success("Written successfully")
                        }
                    }
                }
            } catch (_: SecurityException) {
                NfcResult.Error("The tag was removed too quickly. Hold it still and try again.")
            } catch (_: IOException) {
                NfcResult.Error("The tag connection was lost while writing.")
            } catch (error: Exception) {
                NfcResult.Error(error.message ?: "The tag could not be written.")
            } finally {
                runCatching { ndef.close() }
            }
        }

        val formatable = NdefFormatable.get(tag)
            ?: return NfcResult.Error("This tag does not support writable NDEF data.")
        return try {
            formatable.connect()
            formatable.format(message)
            NfcResult.Success(if (verify) "Written successfully; this newly formatted tag could not be re-read in the same tap." else "Written successfully")
        } catch (_: IOException) {
            NfcResult.Error("The tag could not be formatted. It may be locked or was removed too quickly.")
        } catch (error: Exception) {
            NfcResult.Error(error.message ?: "The tag could not be formatted.")
        } finally {
            runCatching { formatable.close() }
        }
    }

    fun makeReadOnly(tag: Tag): NfcResult<String> {
        val ndef = Ndef.get(tag)
            ?: return NfcResult.Error("This tag does not expose standard NDEF data and cannot be locked by this app.")
        return try {
            ndef.connect()
            when {
                !ndef.isWritable -> NfcResult.Error("This tag is already read-only.")
                !ndef.canMakeReadOnly() -> NfcResult.Error("This tag does not support being made permanently read-only.")
                // Android delegates this irreversible operation to the tag technology.
                // There is no API to undo it after the tag confirms the lock command.
                ndef.makeReadOnly() -> NfcResult.Success("NFC tag is now permanently read-only")
                else -> NfcResult.Error("The tag did not confirm the permanent read-only operation.")
            }
        } catch (_: SecurityException) {
            NfcResult.Error("The tag was removed too quickly. Hold it still and try again.")
        } catch (_: IOException) {
            NfcResult.Error("The tag connection was lost before it could be locked.")
        } catch (error: Exception) {
            NfcResult.Error(error.message ?: "The tag could not be made read-only.")
        } finally {
            runCatching { ndef.close() }
        }
    }
}
