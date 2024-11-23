package top.phj233.magplay.ui.screens.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.phj233.magplay.entity.Contact
import top.phj233.magplay.repository.DBUtil

class ContactViewModel : ViewModel() {
    private val contactDao = DBUtil.getContactDao()

    val contacts: StateFlow<List<Contact>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertContact(contact: Contact) {
        viewModelScope.launch {
            contactDao.insertContact(contact)
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            contactDao.updateContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            contactDao.deleteContact(contact)
        }
    }
}