/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ktor.start.intellij.util

import com.intellij.openapi.application.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.ui.components.labels.*
import com.intellij.uiDesigner.core.*
import io.ktor.start.util.*
import java.awt.*
import java.io.*
import java.net.*
import javax.swing.*

operator fun VirtualFile?.get(path: String?): VirtualFile? {
    if (this == null || path == null || path == "" || path == ".") return this
    val parts = path.split('/', limit = 2)
    val firstName = parts[0]
    val lastName = parts.getOrNull(1)
    val child = this.findChild(firstName)
    return if (lastName != null) child[lastName] else child
}

fun VirtualFile.createFile(path: String, data: String, charset: Charset = UTF8, mode: Int = "0644".toInt(radix = 8)): VirtualFile =
    createFile(path, data.toByteArray(charset), mode)

fun VirtualFile.createFile(path: String, data: ByteArray, mode: Int = "0644".toInt(radix = 8)): VirtualFile {
    val file = PathInfo(path)
    val fileParent = file.parent
    val dir = this.createDirectories(fileParent)
    return runWriteAction {
        dir.createChildData(null, file.name).apply {
            setBinaryContent(data)
            if (isInLocalFileSystem) {
                canonicalPath?.let { cp ->
                    val lfile = File(cp)
                    if (lfile.exists()) {
                        if (((mode ushr 6) and 1) != 0) { // Executable bit on the user part
                            lfile.setExecutable(true)
                        }
                    }
                }
            }
        }
    }
}

class PathInfo(val path: String) {
    val name: String get() = path.substringAfterLast('/', path)
    val parent: String? get() = if (path.contains('/')) path.substringBeforeLast('/', "") else null
}

fun VirtualFile.createDirectories(path: String?): VirtualFile {
    if (path == null) return this
    return runWriteAction {
        val parts = path.split('/', limit = 2)
        val firstName = parts[0]
        val lastName = parts.getOrNull(1)
        val child = this.findChild(firstName) ?: this.createChildDirectory(null, firstName)
        if (lastName != null) child.createDirectories(lastName) else child
    }
}

fun Project.backgroundTask(
    name: String,
    indeterminate: Boolean = true,
    cancellable: Boolean = false,
    background: Boolean = false,
    callback: (indicator: ProgressIndicator) -> Unit
) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(this, name, cancellable, { background }) {
        override fun shouldStartInBackground() = background

        override fun run(indicator: ProgressIndicator) {
            try {
                if (indeterminate) indicator.isIndeterminate = true
                callback(indicator)
            } catch (e: Throwable) {
                e.printStackTrace()
                throw e
            }
        }
    })
}

inline fun <T> runWriteAction(crossinline runnable: () -> T): T {
    //return ApplicationManager.getApplication().runWriteAction(Computable { runnable() })
    return object : WriteAction<T>() {
        @Throws(Throwable::class)
        override fun run(result: Result<T>) {
            val res = runnable()
            result.setResult(res)
        }
    }.execute().throwException().resultObject
}

val <T> JComboBox<T>.selected get() = selectedItem as T

fun JPanel.addAtGrid(
    item: JComponent,
    row: Int, column: Int,
    rowSpan: Int = 1, colSpan: Int = 1,
    anchor: Int = GridConstraints.ANCHOR_CENTER,
    fill: Int = GridConstraints.FILL_NONE,
    HSizePolicy: Int = GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_CAN_SHRINK,
    VSizePolicy: Int = GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_CAN_SHRINK,
    minimumSize: Dimension = Dimension(-1, -1),
    preferredSize: Dimension = Dimension(-1, -1),
    maximumSize: Dimension = Dimension(-1, -1)
) {
    add(
        item,
        GridConstraints(
            row,
            column,
            rowSpan,
            colSpan,
            anchor,
            fill,
            HSizePolicy,
            VSizePolicy,
            minimumSize,
            preferredSize,
            maximumSize
        )
    )
}

inline fun invokeLater(crossinline func: () -> Unit) {
    if (ApplicationManager.getApplication().isDispatchThread) {
        func()
    } else {
        ApplicationManager.getApplication().invokeLater({ func() }, ModalityState.defaultModalityState())
    }
}

fun Component.scrollVertical() = ScrollPaneFactory.createScrollPane(
    this,
    JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
    JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
)

fun Component.scrollHorizontal() = ScrollPaneFactory.createScrollPane(
    this,
    JBScrollPane.VERTICAL_SCROLLBAR_NEVER,
    JBScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
)

fun Component.scrollBoth() = ScrollPaneFactory.createScrollPane(
    this,
    JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
    JBScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
)

fun Link(text: String, url: URL) = LinkLabel<URL>(text, null, { _, data ->
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(data.toURI())
    }
}, url)
