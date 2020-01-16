import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object NonStop {

    var optionVerbose = false

    fun processSvg(rootNodes: NodeList) {
        val defsNode = rootNodes.findNode("defs")
        if (defsNode == null) {
            println("No 'defs' node found, possibly this file does not contain gradients, exiting.")
            return
        }

        val gradientNodes = defsNode.childNodes
        val gradientStopsMap = mutableMapOf<String, List<Node>>()
        val targetGradientsMap = mutableMapOf<String, MutableList<Node>>()

        gradientNodes.forEach {
            // Skipping whitespace nodes
            if (it.nodeName == "#text") return@forEach

            logv("Processing gradient node ${it.nodeName}")

            val xlinkedId = it.getAttribute("xlink:href")?.removePrefix("#")
            if (xlinkedId != null) {
                // Links to gradients
                if (targetGradientsMap[xlinkedId] == null) targetGradientsMap[xlinkedId] = mutableListOf()
                targetGradientsMap[xlinkedId]!!.add(it)
                logv("Found linked gradient, id $xlinkedId")
            } else if (it.nodeName == "linearGradient") {
                // Linear gradients with <stop> tags
                val stopNodes = mutableListOf<Node>()
                it.childNodes.forEach { stopNode -> if (stopNode.nodeName == "stop") stopNodes.add(stopNode) }
                if (stopNodes.isEmpty()) return@forEach

                val id = it.getId()
                logv("Found linear gradient with stops, id $id")
                gradientStopsMap[id] = stopNodes
            }
        }

        println("Inserting stops into target gradients.")
        targetGradientsMap.forEach { targetGradientsMapEntry ->
            val targetGradientId = targetGradientsMapEntry.key
            val stops = gradientStopsMap[targetGradientId] ?: return@forEach
            val targetGradients = targetGradientsMapEntry.value
            logv("Inserting stops for id $targetGradientId, ${targetGradients.count()} gradient(s) count")
            targetGradients.forEach { targetGradient ->
                stops.forEach { stop ->
                    logv("Inserting stop ${stop.getId()} for target gradient ${targetGradient.getId()}")
                    // appendChild will move node if attached to document already, cloning then
                    targetGradient.appendChild(stop.cloneNode(false))
                }
            }
        }
    }

    private fun NodeList.forEach(action: (Node) -> Unit) {
        for (i in 0 until this.length) action.invoke(this.item(i))
    }

    private fun NodeList.findNode(name: String): Node? {
        for (i in 0 until this.length) {
            val node = this.item(i)
            if (node.nodeName == name) return node
        }
        return null
    }

    private fun NamedNodeMap.hasNode(name: String): Boolean {
        for (i in 0 until this.length) {
            val node = this.item(i)
            if (node.nodeName == name) return true
        }
        return false
    }

    private fun Node.getId(): String = getAttribute("id")!!

    private fun Node.getAttribute(attrName: String): String? {
        return this.attributes.getNamedItem(attrName)?.nodeValue
    }

    private fun logv(message: String) {
        if (optionVerbose) println(message)
    }
}


fun main(args: Array<String>) {
    var filename: String? = null
    var optionForceOverwrite = false
    args.forEach {
        when (it) {
            "-f" -> {
                optionForceOverwrite = true
                println("Option Force overwrite is on.")
            }
            "-v" -> {
                NonStop.optionVerbose = true
                println("Option Verbose is on.")
            }
            else -> filename = it
        }
    }
    if (filename == null) {
        println("Pass target SVG file name as parameter.\n" +
                "Add -f to force overwrite target _nonstop.svg file, -v to have verbose output.")
        return
    }

    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = documentBuilder.parse(filename)
    val documentURI = document.documentURI

    val nonstopDocumentURI = documentURI.replace(".svg", "_nonstop.svg")
    val nonstopFile = File(URI.create(nonstopDocumentURI))
    if (nonstopFile.exists() && !optionForceOverwrite) {
        println("Target file ${nonstopFile.name} already exists, exiting. Use -f option to force overwrite.")
        return
    }

    println("Parsed file $documentURI")
    val nodes = document.documentElement.childNodes
    println("Document contains ${nodes.length} nodes.")

    NonStop.processSvg(nodes)

    println("Processed document.")
    val domSource = DOMSource(document)
    val streamResult = StreamResult(nonstopFile)
    TransformerFactory.newInstance().newTransformer().transform(domSource, streamResult)
    println("Saved new file $nonstopDocumentURI successfully.")
}