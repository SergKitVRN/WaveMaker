package com.example.wavemaker

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow


class MainActivity : AppCompatActivity() {
    // frequency, double fadingSec, double durationSec   echoDurationSec,  echoVolume,  waveform
    private var playing: Boolean = false

    private val MAX_DURATION_SEC: Double = 3.0
    private var volume: Double = 0.5
    private var frequency: Double = 1000.0
    private var sensitivity:Double = 0.5
    private var durationSec: Double = 0.1
    private var fadingSec: Double = 0.1
    private var echoDurationSec: Double = 0.5
    private var echoVolume: Double = 0.5
    private var waveform: Int = 1
    private var useSensor: Int = 1
    private var noteNum: Int = 0
    private var intermediateFlag = false
    private var continuouslyFlag = false
    private var useNotesFlag = false

    private val octaveNotesNames: Array<String> =
        arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B(H)")
    private val octaveMainNotesNames: Array<String> = arrayOf("C", "D", "E", "F", "G", "A", "B(H)")
    private var mainTonesNotesFreq = emptyArray<Double>()
    private var allNotesFreq = emptyArray<Double>()
    private var notesFreq = allNotesFreq

    private var noteMin: Int = 14
    private var noteMax: Int = 35

    lateinit var sensorManager: SensorManager
    var accelerationSensor: Sensor? = null
    var magnetSensor: Sensor? = null
    var luminositySensor: Sensor? = null
    var currentSensor: Sensor? = null

    private lateinit var playStopButton: Button
    private lateinit var continuouslyCheckBox: CheckBox
    private lateinit var useNotesCheckBox: CheckBox
    private lateinit var intermediateCheckBox: CheckBox
    private lateinit var controlByRadioGroup: RadioGroup
    private lateinit var luminosityRadioButton: RadioButton
    private lateinit var magnetRadioButton: RadioButton
    private lateinit var accelerationRadioButton: RadioButton
    private lateinit var maxNumberPicker: NumberPicker
    private lateinit var minNumberPicker: NumberPicker
    private lateinit var minNoteTextView: TextView
    private lateinit var maxNoteTextView: TextView
    private lateinit var echoDurationTextView: TextView
    private lateinit var echoVolumeTextView: TextView
    private lateinit var sensitivityTextView: TextView
    private lateinit var soundDurationTextView: TextView
    private lateinit var soundFadingTextView: TextView
    private lateinit var echoDurationSeekBar: SeekBar
    private lateinit var echoVolumeSeekBar: SeekBar
    private lateinit var soundDurationSeekBar: SeekBar
    private lateinit var sensitivitySeekBar: SeekBar
    private lateinit var soundFadingSeekBar: SeekBar
    private lateinit var waveformRadioGroup1: RadioGroup
    private lateinit var waveformRadioGroup2: RadioGroup
    private lateinit var freqTextView: TextView

    private var sensorTypeChanged: Boolean = true
    private var prevSensorValues: Array<Double> = arrayOf(0.0, 0.0, 0.0)
    private var magnetSensorZeros: Array<Double> = arrayOf(0.0, 0.0, 0.0)
    private var luminositySensorMaximum: Double = 1.0

    private fun floatToRainbow(f:Double):Int{
        val a:Double = (1.0 - f) / 0.2
        val y:Int = (255.0 * (a - a.toInt().toDouble())).toInt()
        var r:Int = 0
        var g:Int = 0
        var b:Int = 0

        when (a.toInt()) {
            0 -> {
                r = 255
                g = y
                b = 0
            }
            1 -> {
                r = 255 - y
                g = 255
                b = 0
            }
            2 -> {
                r = 0
                g = 255
                b = y
            }
            3 -> {
                r = 0
                g = 255 - y
                b = 255
            }
            4 -> {
                r = y
                g = 0
                b = 255
            }
            5 -> {
                r = 255
                g = 0
                b = 255 - y
            }
        }
        return ((r shr 16) or (g shr 8) or b)
    }

    private fun updateNumberPicker(piker: NumberPicker, values: Array<Double>) {
        var titles = emptyArray<String>()
        for (v in values) {
            titles += String.format(Locale.ENGLISH, "%.1f", v)
        }
        piker.maxValue = values.size - 2
        piker.minValue = 0
        piker.wrapSelectorWheel = false
        piker.displayedValues = titles
    }

    private fun saveSettings() {
        val sharedPref = this?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putFloat(getString(R.string.saved_echo_volume_key), echoVolume.toFloat())
            putFloat(getString(R.string.saved_echo_duration_key), echoDurationSec.toFloat())
            putFloat(getString(R.string.saved_sensitivity_key), sensitivity.toFloat())
            putFloat(getString(R.string.saved_sound_duration_key), durationSec.toFloat())
            putFloat(getString(R.string.saved_sound_fading_key), fadingSec.toFloat())
            putInt(getString(R.string.saved_waveform_key), waveform)
            putInt(getString(R.string.saved_sensor_key), useSensor)
            putInt(getString(R.string.saved_min_note_key), noteMin)
            putInt(getString(R.string.saved_max_note_key), noteMax)
            putBoolean(getString(R.string.saved_intermediate_key), intermediateFlag)
            putBoolean(getString(R.string.saved_continuously_key), continuouslyFlag)
            putBoolean(getString(R.string.saved_use_notes_key), useNotesFlag)
            apply()
        }
    }

    private fun loadSettings() {
        val sharedPref = this?.getPreferences(Context.MODE_PRIVATE) ?: return
        echoVolume = sharedPref.getFloat(getString(R.string.saved_echo_volume_key), 0.5F).toDouble()
        echoDurationSec = sharedPref.getFloat(getString(R.string.saved_echo_duration_key), 0.5F).toDouble()
        sensitivity = sharedPref.getFloat(getString(R.string.saved_sensitivity_key), 0.5F).toDouble()
        durationSec = sharedPref.getFloat(getString(R.string.saved_sound_duration_key), 0.5F).toDouble()
        fadingSec = sharedPref.getFloat(getString(R.string.saved_sound_fading_key), 0.5F).toDouble()
        waveform = sharedPref.getInt(getString(R.string.saved_waveform_key), 1)
        noteMin = sharedPref.getInt(getString(R.string.saved_min_note_key), 20)
        noteMax = sharedPref.getInt(getString(R.string.saved_max_note_key), 30)
        intermediateFlag = sharedPref.getBoolean(getString(R.string.saved_intermediate_key), false)
        continuouslyFlag = sharedPref.getBoolean(getString(R.string.saved_continuously_key), false)
        useNotesFlag = sharedPref.getBoolean(getString(R.string.saved_use_notes_key), false)
        if (intermediateFlag) {
            notesFreq = allNotesFreq
        } else {
            notesFreq = mainTonesNotesFreq
        }
        useSensor = sharedPref.getInt(getString(R.string.saved_sensor_key), 1)
        when (useSensor) {
            0 -> {
                currentSensor = magnetSensor
            }
            1 -> {
                currentSensor = accelerationSensor
            }
            2 -> {
                currentSensor = luminositySensor
            }
        }
    }

    private fun listOfRadiobuttons(radioGroup:RadioGroup):ArrayList<RadioButton> {
        val listOfRadioButtons = ArrayList<RadioButton>()
        for (i in 0 until radioGroup.childCount) {
            val o: View = radioGroup.getChildAt(i)
            if (o is RadioButton) {
                listOfRadioButtons.add(o as RadioButton)
            }
        }
        return listOfRadioButtons
    }

    private fun setRadioGroupCheckedByTag(radioGroup:RadioGroup, tag:Int) {
        for (i in 0 until radioGroup.childCount) {
            val o: View = radioGroup.getChildAt(i)
            if (o is RadioButton) {
                if (o.tag.toString().toInt() == tag) {
                    o.isChecked = true
                    break
                }
            }
        }
    }

    private fun getRadioGroupCheckedTag(radioGroup:RadioGroup):Int {
        var checkedId = radioGroup.checkedRadioButtonId
        if (checkedId == -1) {
            return -1
        }
        return findViewById<RadioButton>(checkedId).tag.toString().toInt()
    }

    private fun getParametersFromGUI() {
        waveform = getRadioGroupCheckedTag(waveformRadioGroup1)
        if (waveform == -1) {
            waveform = getRadioGroupCheckedTag(waveformRadioGroup2)
        }
        echoDurationSec = MAX_DURATION_SEC * echoDurationSeekBar.progress.toDouble() / 100.0
        echoVolume = echoVolumeSeekBar.progress.toDouble() / 100.0
        durationSec = MAX_DURATION_SEC * soundDurationSeekBar.progress.toDouble() / 100.0
        fadingSec = MAX_DURATION_SEC * soundFadingSeekBar.progress.toDouble() / 100.0
        sensitivity = sensitivitySeekBar.progress.toDouble() / 100.0
        continuouslyFlag = continuouslyCheckBox.isChecked
        useNotesFlag = useNotesCheckBox.isChecked
        intermediateFlag = intermediateCheckBox.isChecked
        noteMin = minNumberPicker.value
        noteMax = maxNumberPicker.value
        useSensor = findViewById<RadioButton>(controlByRadioGroup.checkedRadioButtonId).tag.toString().toInt()
        when (useSensor) {
            0 -> {
                currentSensor = magnetSensor
            }
            1->{
                currentSensor = accelerationSensor
            }
            2 -> {
                currentSensor = luminositySensor
            }
        }
        setSettings(echoDurationSec, echoVolume, waveform)
    }

    private fun showParametersOnGUI(){
        var guiWaveform = getRadioGroupCheckedTag(waveformRadioGroup1)
        if (guiWaveform == -1) {
            guiWaveform = getRadioGroupCheckedTag(waveformRadioGroup2)
        }
        if (guiWaveform != waveform) {
            setRadioGroupCheckedByTag(waveformRadioGroup1, waveform)
            setRadioGroupCheckedByTag(waveformRadioGroup2, waveform)
        }

        if (notesFreq == allNotesFreq) {
            minNoteTextView.text = octaveNotesNames[noteMin % 12] + (noteMin / 12).toString()
            maxNoteTextView.text = octaveNotesNames[noteMax % 12] + (noteMax / 12).toString()
        } else {
            minNoteTextView.text = octaveMainNotesNames[noteMin % 7] + (noteMin / 7).toString()
            maxNoteTextView.text = octaveMainNotesNames[noteMax % 7] + (noteMax / 7).toString()
        }

        val guiEchoDurationSec = MAX_DURATION_SEC * echoDurationSeekBar.progress.toDouble() / 100.0
        if (guiEchoDurationSec != echoDurationSec) {echoDurationSeekBar.progress = (100.0 * echoDurationSec / MAX_DURATION_SEC).toInt()}

        val guiEchoVolume = echoVolumeSeekBar.progress.toDouble() / 100.0
        if (guiEchoVolume != echoVolume) {echoVolumeSeekBar.progress = (100.0 * echoVolume).toInt()}

        val guiDurationSec = MAX_DURATION_SEC * soundDurationSeekBar.progress.toDouble() / 100.0
        if (guiDurationSec != durationSec) {soundDurationSeekBar.progress = (100.0 * durationSec / MAX_DURATION_SEC).toInt()}

        val guiFadingSec = MAX_DURATION_SEC * soundFadingSeekBar.progress.toDouble() / 100.0
        if (guiFadingSec != fadingSec) {soundFadingSeekBar.progress = (100.0 * fadingSec / MAX_DURATION_SEC).toInt()}

        val guiSensitivity = sensitivitySeekBar.progress.toDouble() / 100.0
        if (guiSensitivity != sensitivity) { sensitivitySeekBar.progress = (100.0 * sensitivity).toInt() }

        val guiNoteMin = minNumberPicker.value
        if (guiNoteMin != noteMin) { minNumberPicker.value = noteMin }

        val guiNoteMax = maxNumberPicker.value
        if (guiNoteMax != noteMax) { maxNumberPicker.value = noteMax }

        val guiUseSensor = findViewById<RadioButton>(controlByRadioGroup.checkedRadioButtonId).tag.toString().toInt()
        if (guiUseSensor != useSensor) {
            setRadioGroupCheckedByTag(controlByRadioGroup, useSensor)
        }
        echoDurationTextView.text = String.format(Locale.ENGLISH, "%.0f", echoDurationSec * 1000.0)
        echoVolumeTextView.text = String.format(Locale.ENGLISH, "%.0f", echoVolume * 100.0)
        soundDurationTextView.text = String.format(Locale.ENGLISH, "%.0f", durationSec * 1000.0)
        soundFadingTextView.text = String.format(Locale.ENGLISH, "%.0f", fadingSec * 1000.0)
        sensitivityTextView.text = String.format(Locale.ENGLISH, "%.0f", sensitivity * 100.0)
        useNotesCheckBox.isChecked = useNotesFlag
        intermediateCheckBox.isChecked = intermediateFlag
        continuouslyCheckBox.isChecked = continuouslyFlag
        //setSettings(echoDurationSec, echoVolume, waveform)
        //generateSound(1000.0, fadingSec, durationSec)

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playStopButton = findViewById<Button>(R.id.buttonPlayStop)
        continuouslyCheckBox = findViewById<CheckBox>(R.id.checkBoxContinuously)
        useNotesCheckBox = findViewById<CheckBox>(R.id.checkBoxUseNotes)
        intermediateCheckBox = findViewById<CheckBox>(R.id.checkBoxIntermediate)
        controlByRadioGroup = findViewById<RadioGroup>(R.id.radioGroupControlBy)
        luminosityRadioButton = findViewById<RadioButton>(R.id.radioButtonLuminosity)
        magnetRadioButton = findViewById<RadioButton>(R.id.radioButtonMagnet)
        accelerationRadioButton = findViewById<RadioButton>(R.id.radioButtonMoving)
        maxNumberPicker = findViewById<NumberPicker>(R.id.numberPickerMax)
        minNumberPicker = findViewById<NumberPicker>(R.id.numberPickerMin)
        minNoteTextView = findViewById<TextView>(R.id.textViewMinNote)
        maxNoteTextView = findViewById<TextView>(R.id.textViewMaxNote)
        echoDurationTextView = findViewById<TextView>(R.id.textViewEchoDurationVal)
        echoVolumeTextView = findViewById<TextView>(R.id.textViewEchoVolumeVal)
        sensitivityTextView = findViewById<TextView>(R.id.textViewSensitivityVal)
        soundDurationTextView = findViewById<TextView>(R.id.textViewSoundDurationVal)
        soundFadingTextView = findViewById<TextView>(R.id.textViewSoundFadingVal)
        echoDurationSeekBar = findViewById<SeekBar>(R.id.seekBarEchoDuration)
        echoVolumeSeekBar = findViewById<SeekBar>(R.id.seekBarEchoVolume)
        soundDurationSeekBar = findViewById<SeekBar>(R.id.seekBarSoundDuration)
        sensitivitySeekBar = findViewById<SeekBar>(R.id.seekBarSensitivity)
        soundFadingSeekBar = findViewById<SeekBar>(R.id.seekBarSoundFading)
        waveformRadioGroup1 = findViewById<RadioGroup>(R.id.radioGroupWaveform1)
        waveformRadioGroup2 = findViewById<RadioGroup>(R.id.radioGroupWaveform2)
        freqTextView = findViewById<TextView>(R.id.textureView)
        for (i in 0 until 10 * 12) {
            allNotesFreq += 440.0 * 2.0.pow((i - 48 - 9).toDouble() / 12.0)
            if (arrayOf(0, 2, 4, 5, 7, 9, 11).contains(i % 12)) {
                mainTonesNotesFreq += allNotesFreq[i]
            }
        }
        startEngine()
        setVolume(0.5)
        loadSettings()
        showParametersOnGUI()

        lateinit var listener1: RadioGroup.OnCheckedChangeListener
        lateinit var listener2: RadioGroup.OnCheckedChangeListener

        listener1 = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            waveformRadioGroup2.setOnCheckedChangeListener({ _, _ -> })
            waveformRadioGroup2.clearCheck()
            waveformRadioGroup2.setOnCheckedChangeListener(listener2)
            getParametersFromGUI()
            if (!playing) { generateSound(1000.0, fadingSec, durationSec) }
        }
        listener2 = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            waveformRadioGroup1.setOnCheckedChangeListener({ _, _ -> })
            waveformRadioGroup1.clearCheck()
            waveformRadioGroup1.setOnCheckedChangeListener(listener1)
            getParametersFromGUI()
            if (!playing) {generateSound(1000.0, fadingSec, durationSec) }
        }
        waveformRadioGroup1.setOnCheckedChangeListener(listener1)
        waveformRadioGroup2.setOnCheckedChangeListener(listener2)

        controlByRadioGroup.setOnCheckedChangeListener {group, checkedId ->
            getParametersFromGUI()
        }

        updateNumberPicker(minNumberPicker, notesFreq)
        updateNumberPicker(maxNumberPicker, notesFreq)
        minNumberPicker.value = noteMin
        maxNumberPicker.value = noteMax

        minNumberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            if (newVal >= noteMax) {
                maxNumberPicker.value = newVal
                noteMax = newVal
            }
            noteMin = newVal
            if (!playing) { generateSound(notesFreq[noteMin], fadingSec, durationSec) }
            showParametersOnGUI()
        }
        maxNumberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            if (newVal <= noteMin) {
                minNumberPicker.value = newVal
                noteMin = newVal
            }
            noteMax = newVal
            if (!playing) { generateSound(notesFreq[noteMax], fadingSec, durationSec) }
            showParametersOnGUI()
        }

        intermediateCheckBox.setOnCheckedChangeListener { checkBox, newVal ->
            getParametersFromGUI()
            if (newVal) {
                notesFreq = allNotesFreq
                noteMin = (12.0 * noteMin.toDouble() / 7.0).toInt() + 1
                noteMax = (12.0 * noteMax.toDouble() / 7.0).toInt() + 1
            } else {
                notesFreq = mainTonesNotesFreq
                noteMin = (7.0 * noteMin.toDouble() / 12.0).toInt()
                noteMax = (7.0 * noteMax.toDouble() / 12.0).toInt()
            }
            minNumberPicker.value = noteMin
            maxNumberPicker.value = noteMax
            updateNumberPicker(minNumberPicker, notesFreq)
            updateNumberPicker(maxNumberPicker, notesFreq)
            showParametersOnGUI()
        }

        useNotesCheckBox.setOnCheckedChangeListener { checkBox, newVal ->
            getParametersFromGUI()
        }

        continuouslyCheckBox.setOnCheckedChangeListener { checkBox, newVal ->
            getParametersFromGUI()
        }

        echoDurationSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                getParametersFromGUI()
                showParametersOnGUI()
                setSettings(echoDurationSec, echoVolume, waveform)
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                if (!playing) { generateSound(1000.0, fadingSec, durationSec) }
                // write custom code for progress is stopped
            }
        })

        echoVolumeSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                getParametersFromGUI()
                showParametersOnGUI()
                setSettings(echoDurationSec, echoVolume, waveform)
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                if (!playing) { generateSound(1000.0, fadingSec, durationSec) }
            }
        })

        soundDurationSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                getParametersFromGUI()
                showParametersOnGUI()
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                if (!playing) { generateSound(1000.0, fadingSec, durationSec)}
            }
        })

        sensitivitySeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                getParametersFromGUI()
                showParametersOnGUI()
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
            }
        })

        soundFadingSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                getParametersFromGUI()
                showParametersOnGUI()
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                if (!playing) { generateSound(1000.0, fadingSec, durationSec)}
            }
        })

        freqTextView.setOnClickListener {
            Toast.makeText(applicationContext, "(c) Sergey Kitaev 2022.\nhttps://t.me/SergVrn", Toast.LENGTH_LONG).show()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        luminositySensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        var sensorEventListener: SensorEventListener? = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // method to check accuracy changed in sensor.
            }

            override fun onSensorChanged(event: SensorEvent) {
                var newNoteNum: Int = noteNum
                var valForNoteNum: Double = 0.0
                var noteWasChanged:Boolean = false
                when (event.sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (sensorTypeChanged) {
                            sensorTypeChanged = false
                            magnetSensorZeros[0] = event.values[0].toDouble()
                            magnetSensorZeros[1] = event.values[1].toDouble()
                            magnetSensorZeros[2] = event.values[2].toDouble()
                        }
                        var a: Array<Double> = arrayOf(
                            0.1 * sensitivity * (event.values[0].toDouble() - magnetSensorZeros[0]),
                            0.1 * sensitivity * (event.values[1].toDouble() - magnetSensorZeros[1]),
                            0.1 * sensitivity * (event.values[2].toDouble() - magnetSensorZeros[2]))
                        valForNoteNum = a[0] * a[1]
                        volume = a[2]
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        var a: Array<Double> = arrayOf(
                            sensitivity * (event.values[0].toDouble() + 9.82) / 9.82,
                            sensitivity * (event.values[1].toDouble() + 9.82) / 9.82,
                            (event.values[2].toDouble() + 9.82) / 9.82)
                        valForNoteNum = a[0]
                        volume = a[2]
                    }
                    Sensor.TYPE_LIGHT -> {
                        if (sensorTypeChanged) {
                            sensorTypeChanged = false
                            luminositySensorMaximum = event.values[0].toDouble()
                        }
                        valForNoteNum = sensitivity * event.values[0].toDouble() / luminositySensorMaximum
                        volume = 1.0
                    }
                }
                volume = min(abs(volume), 1.0)
                valForNoteNum = min(abs(valForNoteNum), 1.0)
                setVolume(volume)
                if (useNotesFlag) {
                    val notesRange:Double =  (noteMax - noteMin).toDouble()
                    val noteVal = (noteNum - noteMin).toDouble() / notesRange
                    newNoteNum = (valForNoteNum * notesRange).toInt() + noteMin

                    if ((noteNum > newNoteNum &&  (noteVal - valForNoteNum) > 0.5 / notesRange) ||
                        (noteNum < newNoteNum &&  (valForNoteNum - noteVal) > 0.5 / notesRange)) {
                        noteNum = newNoteNum
                        frequency = notesFreq[newNoteNum]
                        noteWasChanged = true
                    }
                    if (notesFreq == allNotesFreq) {
                        freqTextView.text = String.format(Locale.ENGLISH, "%s - %.1f Hz", octaveNotesNames[noteNum % 12] + (noteNum / 12).toString(), frequency)
                    } else {
                        freqTextView.text = String.format(Locale.ENGLISH, "%s - %.1f Hz", octaveMainNotesNames[noteNum % 7] + (noteNum / 7).toString(), frequency)
                    }
                } else {
                    frequency = (notesFreq[noteMax] - notesFreq[noteMin]) * valForNoteNum + notesFreq[noteMin]
                    freqTextView.text = String.format(Locale.ENGLISH, "%.1f Hz", frequency)
                }
                if (continuouslyFlag) {
                    generateSound(frequency, 1.0, 0.0)
                } else if (noteWasChanged || !useNotesFlag) {
                    generateSound(frequency, fadingSec, durationSec)
                }
                freqTextView.setBackgroundColor(Color.HSVToColor(floatArrayOf(360f * valForNoteNum.toFloat(), volume.toFloat(), 0.5f)))
            }
        }

        fun stopPlaying() {
            playing = false
            playStopButton.text = getString(R.string.btn_play)
            sensorManager.unregisterListener(sensorEventListener)
            if (magnetSensor != null) {
                magnetRadioButton.isEnabled = true
            }
            if (accelerationSensor != null) {
                accelerationRadioButton.isEnabled = true
            }
            if (luminositySensor != null) {
                luminosityRadioButton.isEnabled = true
            }
            generateSound(frequency, 1.0, 1.0)
            freqTextView.setBackgroundColor(Color.GRAY)
            freqTextView.text = "----"
        }

        fun startPlaying() {
            getParametersFromGUI()
            sensorTypeChanged = true
            generateSound(1000.0, 1.0, 1.0)
            setVolume(0.8)
            if (currentSensor != null) {
                sensorManager.registerListener(
                    sensorEventListener,
                    currentSensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            }
            playing = true
            playStopButton.text = getString(R.string.btn_stop)
            for (i in 0 until controlByRadioGroup.getChildCount()) {
                (controlByRadioGroup.getChildAt(i) as RadioButton).isEnabled = false
            }
        }

        stopPlaying()

        playStopButton.setOnClickListener {
            saveSettings()
            if (playing) {
                stopPlaying()
            } else {
                startPlaying()
            }
        }

    }






    /*
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(accelerationSensorEventListener, accelerationSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(magnetSensorEventListener, magnetSensor, SensorManager.SENSOR_DELAY_FASTEST)

        // Example of a call to a native method
        //startEngine()

        val runButton = findViewById<Button>(R.id.buttonPlayStop)
        runButton.setOnClickListener {
           // if (playing == 0) playing = 1 else playing = 0
           // touchEvent(playing)
        }
*/

    /*
    var magnetSensorEventListener: SensorEventListener? = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // method to check accuracy changed in sensor.

        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                val a1 = (event.values[0].toDouble() + 1000.0) / 2000.0
                val a2 = (event.values[1].toDouble() + 1000.0) / 2000.0
                val a3 = (event.values[2].toDouble() + 1000.0) / 2000.0
                //if (playing == 0){
                //    a01 = a1;
                //    a02 = a2;
                //    a03 = a3;
                //}

                //val f1:Double = 110.0 + 110.0 * round((a1 - a1) * notesCount)
                //val f2:Double = 10.0
                //currentVolume = a2-a01

               //val s = String.format(Locale.ENGLISH, "m0=%.2f  m1=%.2f, m2=%.2f", a1, a2, a3)
                //txtMsg.setText(s)
              //  setFrequency(f1, f2)
                //setVolume(currentVolume)
            }
        }
    }

    var accelerationSensorEventListener: SensorEventListener? = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // method to check accuracy changed in sensor.

        }


        override fun onSensorChanged(event: SensorEvent) {
            /*
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val a1 = (event.values[0].toDouble() + 9.82) / (2.0 * 9.82) // 0...1
                val a2 = (event.values[2].toDouble() + 9.82) / (2.0 * 9.82) // 0...1

                val dv = (abs(prevAcceleration1 - a1) + abs(prevAcceleration2 - a2)) * 1.0 - 0.05

                val f1:Double = 110.0 + 110.0 * round(a1 * notesCount)
                val f2:Double = 10.0


                //val s = String.format(Locale.ENGLISH, "[%d]  f1=%.2f  f2=%.2f  dv=%.2f, v=%.2f", playing, f1, f2, dv, currentVolume)
                //txtMsg.setText(s)
                currentVolume  = a2 //+= dv
                if (currentVolume > 1.0) currentVolume = 1.0
                if (currentVolume < 0) currentVolume = 0.0

                prevAcceleration1 = a1
                prevAcceleration2 = a2
                //setFrequency(f1, f2)
                //setVolume(currentVolume)


            }

         */
        }

 */



    override fun onDestroy() {
        getParametersFromGUI()
        saveSettings()
        stopEngine()

        super.onDestroy()
    }
    /**
     * A native method that is implemented by the 'wavemaker' native library,
     * which is packaged with this application.
     */
    private external fun startEngine()
    private external fun stopEngine()
    private external fun setSettings(echoDurationSec:Double, echoVolume:Double, waveform:Int)
    private external fun generateSound(frequency:Double, fadingSec:Double, durationSec:Double)
    private external fun setVolume(volume:Double)

    companion object  WaveMaker {
        // Used to load the 'wavemaker' library on application startup.
        init {
            System.loadLibrary("wavemaker")
        }
    }
}