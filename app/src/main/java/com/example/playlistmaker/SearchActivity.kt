package com.example.playlistmaker

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playlistmaker.data.dto.TrackResponse
import com.example.playlistmaker.data.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageView
    private lateinit var backButton: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var placeholderNothingFound: View
    private lateinit var placeholderError: View
    private lateinit var refreshButton: TextView

    private var lastSearchQuery: String = ""

    private val tracks = mutableListOf<Track>()
    private lateinit var adapter: TrackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initViews()

        adapter = TrackAdapter(tracks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        hideAll()

        backButton.setOnClickListener { finish() }

        clearButton.setOnClickListener {
            searchEditText.text.clear()
            hideKeyboard()
            hideAll()
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                clearButton.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // слушатель Done
        searchEditText.setOnEditorActionListener { v, actionId, event ->
            Log.d("SearchActivity", "setOnEditorActionListener called, actionId = $actionId")
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString().trim()
                Log.d("SearchActivity", "Done pressed, query = '$query'")
                if (query.isNotEmpty()) {
                    lastSearchQuery = query
                    performSearch(query)
                }
                true
            } else {
                false
            }
        }

        refreshButton.setOnClickListener {
            if (lastSearchQuery.isNotEmpty()) {
                performSearch(lastSearchQuery)
            }
        }

        searchEditText.requestFocus()
    }

    private fun initViews() {
        searchEditText = findViewById(R.id.search_edit_text)
        clearButton = findViewById(R.id.clear_button)
        backButton = findViewById(R.id.back_button)
        recyclerView = findViewById(R.id.recyclerView)
        placeholderNothingFound = findViewById(R.id.placeholder_nothing_found)
        placeholderError = findViewById(R.id.placeholder_error)
        refreshButton = findViewById(R.id.refresh_button)
    }

    private fun performSearch(query: String) {
        hideKeyboard()

        RetrofitClient.itunesService.search(query).enqueue(object : Callback<TrackResponse> {
            override fun onResponse(call: Call<TrackResponse>, response: Response<TrackResponse>) {

                if (response.isSuccessful) {
                    val result = response.body()

                    if (result != null) {
                        tracks.clear()

                        val receivedTracks = result.results ?: emptyList()

                        tracks.addAll(receivedTracks.map { dto ->
                            Track(
                                trackName = dto.trackName ?: "",
                                artistName = dto.artistName ?: "",
                                trackTime = formatTrackTime(dto.trackTimeMillis ?: 0),
                                artworkUrl100 = dto.artworkUrl100 ?: ""
                            )
                        })

                        adapter.notifyDataSetChanged()

                        if (tracks.isEmpty()) {
                            showNothingFound()
                        } else {
                            showResults()
                        }
                    } else {
                        showNothingFound()
                    }
                } else {
                    showError()
                }
            }

            override fun onFailure(call: Call<TrackResponse>, t: Throwable) {
                showError()
            }
        })
    }

    private fun formatTrackTime(millis: Long): String {
        val minutes = millis / 1000 / 60
        val seconds = millis / 1000 % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun showResults() {
        recyclerView.visibility = View.VISIBLE
        placeholderNothingFound.visibility = View.GONE
        placeholderError.visibility = View.GONE
    }

    private fun showNothingFound() {
        recyclerView.visibility = View.GONE
        placeholderNothingFound.visibility = View.VISIBLE
        placeholderError.visibility = View.GONE
    }

    private fun showError() {
        recyclerView.visibility = View.GONE
        placeholderNothingFound.visibility = View.GONE
        placeholderError.visibility = View.VISIBLE
    }

    private fun hideAll() {
        recyclerView.visibility = View.GONE
        placeholderNothingFound.visibility = View.GONE
        placeholderError.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }
}