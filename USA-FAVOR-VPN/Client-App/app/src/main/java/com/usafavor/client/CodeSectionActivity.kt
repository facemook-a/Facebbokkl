package com.usafavor.client

import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.b5studios.shared.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.ByteArrayInputStream

/** Owner-authored HTML/CSS/JS tools. No native bridge, credentials, file access or network access. */
class CodeSectionActivity:ComponentActivity(){
 private var licenseWatch:com.google.firebase.firestore.ListenerRegistration?=null
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val api=Backend(this);var section by mutableStateOf<Map<String,Any?>>(emptyMap());var problem by mutableStateOf("")
  lifecycleScope.launch{
   try{
    api.anonymous()
    val id=intent.getStringExtra("section_id")?:error("Choose a section")
    val items=api.call("getConfiguration")["sections"].rows()
    section=items.firstOrNull{it.str("id")==id&&it["enabled"]==true&&it["kind"]=="code"}?:error("This tool is no longer available")
    SectionPolicy.validate(listOf(section))
    licenseWatch=api.watchLicense{runOnUiThread{finish()}}
    while(isActive){delay(30000);api.call("licenseInfo")}
   }catch(e:Exception){problem=e.localizedMessage?:"Unable to open this tool";section=emptyMap()}
  }
  setContent{FavorTheme{Backdrop{Column(Modifier.fillMaxSize().padding(16.dp)){
   TextButton(onClick={finish()}){Text("‹ Back")}
   Text(section.str("title").ifBlank{"Client tool"},color=HackerGreen,style=MaterialTheme.typography.headlineSmall)
   if(problem.isNotBlank())Glass("Tool unavailable"){Text(problem)}
   else if(section.isEmpty())LinearProgressIndicator(Modifier.fillMaxWidth())
   else IsolatedTool(section,Modifier.weight(1f).fillMaxWidth())
  }}}}
 }
 override fun onDestroy(){licenseWatch?.remove();super.onDestroy()}
}
/** JavaScript is intentional for owner-authored tools; CSP and WebView deny network, file/content access and native bridges. */
@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable private fun IsolatedTool(section:Map<String,Any?>,modifier:Modifier){
 var web by remember{mutableStateOf<WebView?>(null)}
 val html=remember(section){
  """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"><style>body{margin:16px;background:#071510;color:#a5ffd0;font:16px sans-serif}button,input{font:inherit;padding:10px;border-radius:10px}button{background:#67ffa8;color:#042c18;border:0}${section.str("css")}</style></head><body>${section.str("html")}<script>${section.str("javascript")}</script></body></html>"""
 }
 AndroidView(modifier=modifier,factory={context->WebView(context).apply{
  web=this
  settings.javaScriptEnabled=true
  settings.allowFileAccess=false;settings.allowContentAccess=false;settings.blockNetworkLoads=true
  settings.domStorageEnabled=false;settings.databaseEnabled=false;settings.javaScriptCanOpenWindowsAutomatically=false
  settings.setSupportMultipleWindows(false);settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
  setBackgroundColor(android.graphics.Color.rgb(7,21,16))
  webViewClient=object:WebViewClient(){
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=true
   override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest)=WebResourceResponse("text/plain","utf-8",ByteArrayInputStream(ByteArray(0)))
  }
  loadDataWithBaseURL("https://client-tool.invalid/",html,"text/html","UTF-8",null)
 }})
 DisposableEffect(Unit){onDispose{web?.stopLoading();web?.destroy();web=null}}
}
