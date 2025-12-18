package com.example.additioapp.ui.dialogs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.additioapp.AdditioApplication
import com.example.additioapp.R
import com.example.additioapp.data.model.ClassNoteEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ClassNotesDialog : BottomSheetDialogFragment() {

    private var classId: Long = -1
    private var className: String = ""
    private var notes: MutableList<ClassNoteEntity> = mutableListOf()
    private lateinit var adapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classId = it.getLong("classId")
            className = it.getString("className", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_notes_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textTitle = view.findViewById<TextView>(R.id.textDialogTitle)
        val textCount = view.findViewById<TextView>(R.id.textNotesCount)
        val recyclerNotes = view.findViewById<RecyclerView>(R.id.recyclerNotes)
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layoutEmptyNotes)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddNote)

        textTitle.text = "Notes: $className"

        adapter = NotesAdapter(
            notes = notes,
            onEdit = { note -> showEditNoteDialog(note) },
            onDelete = { note -> confirmDeleteNote(note) }
        )

        recyclerNotes.layoutManager = LinearLayoutManager(requireContext())
        recyclerNotes.adapter = adapter

        fabAdd.setOnClickListener { showAddNoteDialog() }

        // Load notes
        loadNotes(textCount, layoutEmpty, recyclerNotes)
    }

    private fun loadNotes(textCount: TextView, layoutEmpty: LinearLayout, recyclerNotes: RecyclerView) {
        val repository = (requireActivity().application as AdditioApplication).repository
        
        CoroutineScope(Dispatchers.IO).launch {
            val loadedNotes = repository.getNotesForClassSync(classId)
            withContext(Dispatchers.Main) {
                notes.clear()
                notes.addAll(loadedNotes)
                adapter.notifyDataSetChanged()
                
                textCount.text = "${notes.size} note${if (notes.size != 1) "s" else ""}"
                layoutEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
                recyclerNotes.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun showAddNoteDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter note..."
            minLines = 3
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Note")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isNotEmpty()) {
                    saveNote(ClassNoteEntity(classId = classId, content = content))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditNoteDialog(note: ClassNoteEntity) {
        val editText = EditText(requireContext()).apply {
            setText(note.content)
            minLines = 3
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Note")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isNotEmpty()) {
                    updateNote(note.copy(content = content))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteNote(note: ClassNoteEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ -> deleteNote(note) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNote(note: ClassNoteEntity) {
        val repository = (requireActivity().application as AdditioApplication).repository
        CoroutineScope(Dispatchers.IO).launch {
            repository.insertClassNote(note)
            withContext(Dispatchers.Main) {
                view?.let { loadNotes(
                    it.findViewById(R.id.textNotesCount),
                    it.findViewById(R.id.layoutEmptyNotes),
                    it.findViewById(R.id.recyclerNotes)
                ) }
            }
        }
    }

    private fun updateNote(note: ClassNoteEntity) {
        val repository = (requireActivity().application as AdditioApplication).repository
        CoroutineScope(Dispatchers.IO).launch {
            repository.updateClassNote(note)
            withContext(Dispatchers.Main) {
                view?.let { loadNotes(
                    it.findViewById(R.id.textNotesCount),
                    it.findViewById(R.id.layoutEmptyNotes),
                    it.findViewById(R.id.recyclerNotes)
                ) }
            }
        }
    }

    private fun deleteNote(note: ClassNoteEntity) {
        val repository = (requireActivity().application as AdditioApplication).repository
        CoroutineScope(Dispatchers.IO).launch {
            repository.deleteClassNote(note)
            withContext(Dispatchers.Main) {
                view?.let { loadNotes(
                    it.findViewById(R.id.textNotesCount),
                    it.findViewById(R.id.layoutEmptyNotes),
                    it.findViewById(R.id.recyclerNotes)
                ) }
            }
        }
    }

    // Notes Adapter
    inner class NotesAdapter(
        private val notes: List<ClassNoteEntity>,
        private val onEdit: (ClassNoteEntity) -> Unit,
        private val onDelete: (ClassNoteEntity) -> Unit
    ) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textDate: TextView = view.findViewById(R.id.textNoteDate)
            val textContent: TextView = view.findViewById(R.id.textNoteContent)
            val btnEdit: ImageView = view.findViewById(R.id.btnEditNote)
            val btnDelete: ImageView = view.findViewById(R.id.btnDeleteNote)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val note = notes[position]
            holder.textDate.text = dateFormat.format(Date(note.date))
            holder.textContent.text = note.content
            holder.btnEdit.setOnClickListener { onEdit(note) }
            holder.btnDelete.setOnClickListener { onDelete(note) }
        }

        override fun getItemCount() = notes.size
    }

    companion object {
        fun newInstance(classId: Long, className: String) = ClassNotesDialog().apply {
            arguments = Bundle().apply {
                putLong("classId", classId)
                putString("className", className)
            }
        }
    }
}
