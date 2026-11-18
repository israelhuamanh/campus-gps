package com.tecsup.campusgps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.campusgps.model.LugarConDistancia
import com.tecsup.campusgps.util.formatearDistancia

@Composable
fun ListaLugares(
    lugares: List<LugarConDistancia>,
    seleccionado: Int?,
    onClick: (Int) -> Unit,
    radioMetros: Int = 200,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = lugares,
            key = { it.lugar.id }
        ) { item ->
            val esSeleccionado = item.lugar.id == seleccionado
            val interactionSource = remember { MutableInteractionSource() }
            val estaPresionado by interactionSource.collectIsPressedAsState()

            val escala by animateFloatAsState(
                targetValue = if (estaPresionado) 0.98f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "escalaTarjeta"
            )

            val colorFondo by animateColorAsState(
                targetValue = if (esSeleccionado) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "colorFondoTarjeta"
            )

            val colorBorde by animateColorAsState(
                targetValue = if (esSeleccionado) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "colorBordeTarjeta"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                    }
                    .animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessMedium),
                        placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick(item.lugar.id) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colorFondo),
                border = BorderStroke(
                    width = if (esSeleccionado) 2.dp else 1.dp,
                    color = colorBorde
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (esSeleccionado) 4.dp else 1.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contenedor Avatar con icono de categoría Expressive
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (esSeleccionado) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    contenedorColorPorCategoria(item.lugar.categoria)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconoPorCategoria(item.lugar.categoria),
                            contentDescription = null,
                            tint = if (esSeleccionado) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                textoColorPorCategoria(item.lugar.categoria)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.lugar.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (esSeleccionado) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = item.lugar.categoria,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (esSeleccionado) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            AnimatedVisibility(visible = item.dentroDelRadio) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "En radio ($radioMetros m)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Pill de distancia
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (esSeleccionado) {
                            MaterialTheme.colorScheme.primary
                        } else if (item.metros != null) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = item.metros?.let { formatearDistancia(it) } ?: "—",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (esSeleccionado) {
                                MaterialTheme.colorScheme.onPrimary
                            } else if (item.metros != null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun iconoPorCategoria(categoria: String): ImageVector = when (categoria) {
    "Estudio" -> Icons.AutoMirrored.Rounded.MenuBook
    "Comida" -> Icons.Rounded.Restaurant
    "Eventos" -> Icons.Rounded.Event
    "Servicios" -> Icons.Rounded.Build
    else -> Icons.Rounded.Place
}

private fun contenedorColorPorCategoria(categoria: String): Color = when (categoria) {
    "Estudio" -> Color(0xFFD2E4FF)
    "Comida" -> Color(0xFFFFDCC6)
    "Eventos" -> Color(0xFFF3DAFF)
    "Servicios" -> Color(0xFFC8F2CE)
    else -> Color(0xFFE2E2E9)
}

private fun textoColorPorCategoria(categoria: String): Color = when (categoria) {
    "Estudio" -> Color(0xFF001C37)
    "Comida" -> Color(0xFF381400)
    "Eventos" -> Color(0xFF2B0044)
    "Servicios" -> Color(0xFF00210A)
    else -> Color(0xFF191C20)
}
