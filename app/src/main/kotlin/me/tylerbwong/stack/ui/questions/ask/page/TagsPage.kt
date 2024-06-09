package me.tylerbwong.stack.ui.questions.ask.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import me.tylerbwong.stack.R
import me.tylerbwong.stack.ui.questions.ask.AskQuestionViewModel
import me.tylerbwong.stack.ui.questions.ask.page.AskQuestionPage.Companion.MAX_NUM_TAGS

private const val SEARCH_DELAY_MILLIS = 500L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsPage() {
    val viewModel = viewModel<AskQuestionViewModel>()
    LaunchedEffect(viewModel.searchQuery) {
        delay(SEARCH_DELAY_MILLIS)
        viewModel.fetchPopularTags(viewModel.searchQuery)
    }
    val searchTags by viewModel.tags.observeAsState(initial = emptyList())
    val isSearchTagsVisible by remember(searchTags) {
        derivedStateOf { searchTags.isNotEmpty() }
    }
    val isSelectedTagsVisible by remember(viewModel.selectedTags) {
        derivedStateOf { viewModel.selectedTags.isNotEmpty() }
    }
    AskQuestionDetailsLayout(
        title = stringResource(R.string.tags_page_title),
        description = stringResource(R.string.tags_page_description, MAX_NUM_TAGS),
    ) {
        AnimatedVisibility(visible = isSelectedTagsVisible) {
            Spacer(modifier = Modifier.height(32.dp))
            FlowRow(horizontalArrangement = spacedBy(8.dp)) {
                viewModel.selectedTags.forEach {
                    ElevatedFilterChip(
                        selected = true,
                        onClick = { viewModel.updateSelectedTags(viewModel.selectedTags - it) },
                        label = { Text(text = it.name) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = { Text(text = stringResource(R.string.tags_page_hint)) },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedVisibility(visible = isSearchTagsVisible) {
            Spacer(modifier = Modifier.height(64.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = spacedBy(8.dp),
            ) {
                searchTags.forEach {
                    InputChip(
                        selected = false,
                        onClick = { viewModel.updateSelectedTags(viewModel.selectedTags + it) },
                        label = { Text(text = it.name) },
                    )
                }
            }
        }
    }
}
