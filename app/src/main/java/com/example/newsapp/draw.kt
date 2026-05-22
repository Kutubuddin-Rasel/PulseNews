package com.example.newsapp

import android.graphics.drawable.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun SortDropdownMenu(
) {
    var currentSort by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val sortingOptions = listOf("Popular", "Relevance","publishedAt")
        Button(onClick = { expanded = true }) {
            Text(text = "Sort by: $currentSort")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sortingOptions.forEach { sortOption ->
                DropdownMenuItem(
                    text = { Text(sortOption) },
                    onClick = {
                        currentSort=sortOption
                        expanded = false
                    }
                )
            }
        }
    }

@Composable
fun NewsScreen() {
    var currentSort by remember { mutableStateOf("") }

    // Update sorting logic when user selects a different sort
//    SortDropdownMenu(
//        currentSort = currentSort,
//        onSortSelected = { selectedSort ->
//            currentSort = selectedSort
//            // Trigger data reload with new sorting order, e.g., make an API call
//        }
//    )

    // Your news list here, sorted by currentSort value
}


@Composable
fun PreviewSortDropdownMenu() {
    NewsScreen()
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun draw() {
    Column(modifier = Modifier.fillMaxSize()) {
        // SEARCH ICON
        /*
        Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp, top = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White,
                cursorColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            value = "",
            onValueChange = {

            },
            placeholder = { Text(text = "Search Location", color = Color.White) },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = " ",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
        */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, top = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(value = "Search", onValueChange = {})
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = " ",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()) // Adding horizontal scroll
                .padding(top = 8.dp, start = 8.dp), // Padding around the row
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons
        ) {
            Button(onClick = {}) {
                Text("Business")
            }
            Button(onClick = {}) {
                Text("Entertainment")
            }
            Button(onClick = {}) {
                Text("General")
            }
            Button(onClick = {}) {
                Text("Health")
            }
            Button(onClick = {}) {
                Text("Science")
            }
            Button(onClick = {}) {
                Text("Sports")
            }
            Button(onClick = {}) {
                Text("Technology")
            }
        }
        Card(
            elevation = CardDefaults.elevatedCardElevation(10.dp),
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(Color.White)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                // Using Coil to load image from URL
//            AsyncImage(
//                modifier = Modifier.size(200.dp),
//                model = article.urlToImage,
//                contentDescription = "Article Image"
//            )
                Image(
                    painter = painterResource(R.drawable.l),
                    modifier = Modifier.size(100.dp),
                    contentDescription = "Article Image",
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1f)
                ) {
                    Text("ICC chief prosecutor defends Netanyahu arrest warrant")
                    Text("Mr Khan tells the BBC it is important to show the court will hold all nations to the same standard.")
                    // Text(text = article.content, color = Color.Gray)
                }
            }
        }
        var expanded by remember { mutableStateOf(false) }
        val sortingOptions = listOf("Popular", "Relevance")
        Box(modifier = Modifier.padding(16.dp)) {
            // Button to show dropdown menu
            Button(onClick = { expanded = true }) {
                Text(text = "Sort by: popular")
            }

            // Dropdown menu for sorting options
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sortingOptions.forEach { sortOption ->
                    DropdownMenuItem(
                        text = { Text(sortOption) },
                        onClick = {
                           // onSortSelected(sortOption)
                            expanded = false
                        }
                    )
                }
            }
        }
        val sortlist = listOf("relevancy","popularity","publishedAt")
        val isExpanded = remember { mutableStateOf(false) }
        val selectedTest = remember { mutableStateOf("Nothing") }
        ExposedDropdownMenuBox(expanded = isExpanded.value, onExpandedChange = {isExpanded.value=!isExpanded.value}) {
            TextField(modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                value = selectedTest.value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded.value)}
                )
            ExposedDropdownMenu(expanded = isExpanded.value, onDismissRequest = { isExpanded.value=false }) {
                sortlist.forEachIndexed { index, s ->
                    DropdownMenuItem(
                        text = { Text(s) },
                        onClick = {selectedTest.value=sortlist[index]
                            isExpanded.value=false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun bottombar() {
    Box(modifier = Modifier.fillMaxSize()){
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter), horizontalArrangement = Arrangement.SpaceEvenly){
            Button(onClick = {}) {
                Text("top Headlines")
            }
            Button(onClick = {}) {
                Text("everything")
            }

        }
    }
}
