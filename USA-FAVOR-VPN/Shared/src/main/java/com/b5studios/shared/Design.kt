@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.b5studios.shared

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.input.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.*
import kotlin.math.*

val Gold=Color(0xFFF4CD70);val Violet=Color(0xFFAD62FF);val Cyan=Color(0xFF4ADFFF);val Navy=Color(0xFF060918)
val PremiumFont=FontFamily(Font(R.font.cairo,weight=FontWeight.Normal,variationSettings=FontVariation.Settings(FontVariation.weight(400))),Font(R.font.cairo,weight=FontWeight.Medium,variationSettings=FontVariation.Settings(FontVariation.weight(500))),Font(R.font.cairo,weight=FontWeight.SemiBold,variationSettings=FontVariation.Settings(FontVariation.weight(600))),Font(R.font.cairo,weight=FontWeight.Bold,variationSettings=FontVariation.Settings(FontVariation.weight(700))),Font(R.font.cairo,weight=FontWeight.Black,variationSettings=FontVariation.Settings(FontVariation.weight(900))))
val PremiumTypography=Typography().let{t->Typography(displayLarge=t.displayLarge.copy(fontFamily=PremiumFont),displayMedium=t.displayMedium.copy(fontFamily=PremiumFont),displaySmall=t.displaySmall.copy(fontFamily=PremiumFont),headlineLarge=t.headlineLarge.copy(fontFamily=PremiumFont),headlineMedium=t.headlineMedium.copy(fontFamily=PremiumFont),headlineSmall=t.headlineSmall.copy(fontFamily=PremiumFont),titleLarge=t.titleLarge.copy(fontFamily=PremiumFont),titleMedium=t.titleMedium.copy(fontFamily=PremiumFont),titleSmall=t.titleSmall.copy(fontFamily=PremiumFont),bodyLarge=t.bodyLarge.copy(fontFamily=PremiumFont),bodyMedium=t.bodyMedium.copy(fontFamily=PremiumFont),bodySmall=t.bodySmall.copy(fontFamily=PremiumFont),labelLarge=t.labelLarge.copy(fontFamily=PremiumFont),labelMedium=t.labelMedium.copy(fontFamily=PremiumFont),labelSmall=t.labelSmall.copy(fontFamily=PremiumFont))}
val LocalAccent=staticCompositionLocalOf{Violet}
@Composable fun FavorTheme(admin:Boolean=false,content:@Composable ()->Unit){
 val accent=if(admin)Gold else Violet
 MaterialTheme(colorScheme=darkColorScheme(primary=accent,secondary=Cyan,background=Navy,surface=Color(0xFF12172B),onBackground=Color(0xFFF5F1FF),onSurface=Color(0xFFF5F1FF),onPrimary=if(admin)Navy else Color.White,error=Color(0xFFFF8192)),typography=PremiumTypography){
  CompositionLocalProvider(LocalContentColor provides Color(0xFFF5F1FF),LocalAccent provides accent,content=content)
 }
}
@Composable fun Backdrop(content:@Composable BoxScope.()->Unit){
 val motion=rememberInfiniteTransition(label="aurora");val phase by motion.animateFloat(0f,(2*PI).toFloat(),infiniteRepeatable(tween(20000,easing=LinearEasing)),label="drift")
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF140C29),Navy,Color(0xFF061925))))){
  Canvas(Modifier.fillMaxSize()){
   val w=size.width;val h=size.height;val center=Offset(w*(.75f+.14f*sin(phase)),h*.24f)
   drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha=.17f),Color.Transparent),center,w*.9f),w*.9f,center)
   val blue=Offset(w*(.15f+.1f*cos(phase)),h*.77f);drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha=.09f),Color.Transparent),blue,w*.7f),w*.7f,blue)
   for(i in 0..5){val p=Path().apply{moveTo(-w*.2f,h*(.08f+i*.017f));cubicTo(w*.25f,h*(.28f+.02f*sin(phase)),w*.7f,h*.02f,w*1.15f,h*.28f)};drawPath(p,Violet.copy(alpha=.065f+i*.012f),style=Stroke(1.3f))}
   for(i in 0..34){val x=((i*37+11)%101)/100f*w;val y=((i*23+7)%103)/103f*h;drawCircle(if(i%4==0)Gold else Color(0xFFBBC3F2),if(i%3==0)1.6f else .8f,Offset(x,y),alpha=.12f+.12f*(1+sin(phase+i))/2)}
  };content()
 }
}
@Composable fun Brand(admin:Boolean=false,large:Boolean=false){
 val transition=rememberInfiniteTransition(label="brand");val glow by transition.animateFloat(.75f,1f,infiniteRepeatable(tween(2400),RepeatMode.Reverse),label="logo glow")
 Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth().padding(vertical=14.dp)){
  Text("B5",style=TextStyle(brush=Brush.verticalGradient(listOf(Color(0xFFFFF3B0),Gold,Color(0xFFB47C26),Color(0xFFFFE698))),shadow=Shadow(Gold.copy(alpha=.22f*glow),Offset(0f,4f),18f)),fontSize=if(large)60.sp else 29.sp,fontWeight=FontWeight.Black,fontStyle=FontStyle.Italic,letterSpacing=(-4).sp)
  Canvas(Modifier.width(if(large)150.dp else 74.dp).height(7.dp)){drawArc(Gold.copy(alpha=glow),185f,155f,false,style=Stroke(2.5f))}
  Text("S T U D I O S",fontSize=if(large)13.sp else 9.sp,color=Color(0xFFE7E6EF),letterSpacing=2.sp)
  if(large){Spacer(Modifier.height(22.dp));Text(if(admin)"PRIVATE ADMIN PANEL" else "USA FAVOR VPN",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Gold,letterSpacing=.5.sp)}
 }
}
@Composable fun Shield(modifier:Modifier=Modifier){
 val transition=rememberInfiniteTransition(label="shield");val rise by transition.animateFloat(-4f,4f,infiniteRepeatable(tween(2400),RepeatMode.Reverse),label="float")
 Canvas(modifier.size(138.dp).graphicsLayer{translationY=rise}.semantics{contentDescription="USA FAVOR shield"}){
  val w=size.width;val h=size.height
  drawOval(Brush.radialGradient(listOf(Cyan.copy(alpha=.28f),Color.Transparent)),Offset(-w*.1f,h*.72f),Size(w*1.2f,h*.25f))
  val p=Path().apply{moveTo(w*.5f,h*.04f);lineTo(w*.88f,h*.22f);lineTo(w*.83f,h*.63f);quadraticTo(w*.7f,h*.86f,w*.5f,h*.98f);quadraticTo(w*.3f,h*.86f,w*.17f,h*.63f);lineTo(w*.12f,h*.22f);close()}
  drawPath(p,Violet.copy(alpha=.12f),style=Stroke(25f));drawPath(p,Brush.linearGradient(listOf(Color(0xFF44266F),Color(0xFF141C3F),Color(0xFF19586C))));drawPath(p,Brush.linearGradient(listOf(Color(0xFFE6C9FF),Violet,Cyan)),style=Stroke(5f))
  val inner=Path().apply{moveTo(w*.5f,h*.15f);lineTo(w*.77f,h*.29f);lineTo(w*.72f,h*.59f);quadraticTo(w*.65f,h*.74f,w*.5f,h*.86f);quadraticTo(w*.35f,h*.74f,w*.28f,h*.59f);lineTo(w*.23f,h*.29f);close()}
  clipPath(inner){drawRect(Color(0xFFF8F5FF));for(i in 0..7)drawRect(Color(0xFFE7426A),Offset(w*.2f,h*(.24f+i*.086f)),Size(w*.6f,h*.042f));drawRect(Color(0xFF18387F),Offset(w*.22f,h*.23f),Size(w*.3f,h*.33f));for(x in 0..3)for(y in 0..4)drawCircle(Color.White,w*.009f,Offset(w*(.27f+x*.061f),h*(.28f+y*.053f)))}
  drawPath(inner,Gold.copy(alpha=.85f),style=Stroke(2f));drawLine(Color.White.copy(alpha=.5f),Offset(w*.14f,h*.23f),Offset(w*.48f,h*.07f),2f)
 }
}
@Composable fun Skyline(){
 Canvas(Modifier.fillMaxWidth().height(112.dp)){
  val w=size.width;val h=size.height
  drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color(0xFF46204C).copy(alpha=.5f))))
  for(i in 0..36){val x=i*w/37;val bh=h*(.15f+((i*17)%19)/28f);drawRect(Color(0xFF10182E),Offset(x,h-bh),Size(w/40,bh));for(y in 1..10)for(k in 0..1)if(y*7<bh&&(y+k+i)%3!=0)drawRect(if(i%3==0)Violet.copy(alpha=.5f)else Gold.copy(alpha=.42f),Offset(x+3+k*5,h-bh+y*7),Size(2f,3f))}
  val x=w*.76f;val tower=Path().apply{moveTo(x-w*.025f,h);lineTo(x-w*.013f,h*.28f);lineTo(x,h*.15f);lineTo(x+w*.013f,h*.28f);lineTo(x+w*.025f,h);close()};drawPath(tower,Color(0xFF27244D));drawPath(tower,Violet.copy(alpha=.65f),style=Stroke(1.6f));drawLine(Gold.copy(alpha=.8f),Offset(x,0f),Offset(x,h*.3f),1.4f)
  drawLine(Brush.horizontalGradient(listOf(Color.Transparent,Violet,Cyan,Color.Transparent)),Offset(0f,h-2),Offset(w,h-2),2f)
 }
}
@Composable fun Glass(title:String?=null,content:@Composable ColumnScope.()->Unit){
 Column(Modifier.fillMaxWidth().padding(vertical=7.dp).shadow(16.dp,RoundedCornerShape(22.dp),ambientColor=Violet.copy(alpha=.1f),spotColor=Color.Black).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xDD24223E),Color(0xF00D1526)))).border(1.dp,Brush.linearGradient(listOf(Violet.copy(alpha=.48f),Color.White.copy(alpha=.12f),Cyan.copy(alpha=.15f))),RoundedCornerShape(22.dp)).padding(18.dp)){
  if(title!=null){Text(title,color=Gold,fontWeight=FontWeight.SemiBold,fontSize=16.sp);Spacer(Modifier.height(13.dp))};content()
 }
}
@Composable fun Action(label:String,enabled:Boolean=true,onClick:()->Unit){
 val accent=LocalAccent.current;val gold=accent==Gold;val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val scale by animateFloatAsState(if(pressed&&enabled).978f else 1f,label="button press");val depth by animateDpAsState(if(pressed&&enabled)4.dp else 0.dp,label="button depth")
 Box(Modifier.fillMaxWidth().padding(vertical=7.dp).heightIn(min=60.dp).graphicsLayer{scaleX=scale;scaleY=scale;alpha=if(enabled)1f else .45f}){
  Box(Modifier.matchParentSize().padding(top=5.dp).background(if(gold)Color(0xFF795014)else Color(0xFF34105A),RoundedCornerShape(15.dp)))
  Box(Modifier.fillMaxWidth().offset(y=depth).heightIn(min=54.dp).shadow(if(pressed)2.dp else 10.dp,RoundedCornerShape(15.dp),ambientColor=accent.copy(alpha=.3f),spotColor=accent.copy(alpha=.35f)).clip(RoundedCornerShape(15.dp)).background(Brush.verticalGradient(if(gold)listOf(Color(0xFFFFE8A1),Gold,Color(0xFFDFAE44))else listOf(Color(0xFFC881FF),Color(0xFF933AF1),Color(0xFF6B1FC5)))).border(1.dp,if(gold)Color(0xFFFFEFC3)else Color(0xFFDDAAFF),RoundedCornerShape(15.dp)).clickable(enabled=enabled,interactionSource=interaction,indication=LocalIndication.current,role=Role.Button,onClick=onClick).padding(horizontal=16.dp,vertical=15.dp),contentAlignment=Alignment.Center){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){Icon(actionIcon(label),contentDescription=null,tint=if(gold)Color(0xFF221706)else Color.White,modifier=Modifier.size(19.dp));Spacer(Modifier.width(9.dp));Text(label,fontWeight=FontWeight.Bold,color=if(gold)Color(0xFF221706)else Color.White,fontSize=14.sp,letterSpacing=.6.sp)}}
 }
}
@Composable fun Field(label:String,value:String,onValueChange:(String)->Unit,secret:Boolean=false){
 var visible by remember{mutableStateOf(false)}
 OutlinedTextField(value,onValueChange,label={Text(label)},modifier=Modifier.fillMaxWidth().padding(vertical=6.dp),shape=RoundedCornerShape(15.dp),singleLine=!listOf("body","message","help","instructions").any{label.contains(it,true)},maxLines=4,textStyle=TextStyle(color=Color(0xFFF5F1FF),fontSize=16.sp),keyboardOptions=KeyboardOptions(keyboardType=if(secret)KeyboardType.Password else if(label.contains("Email",true))KeyboardType.Email else KeyboardType.Text),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=LocalAccent.current,unfocusedBorderColor=Color(0xFF66607D),focusedContainerColor=Color(0x55101629),unfocusedContainerColor=Color(0x55101629)),visualTransformation=if(secret&&!visible)PasswordVisualTransformation()else VisualTransformation.None,trailingIcon=if(secret){{IconButton(onClick={visible=!visible}){Icon(if(visible)Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,contentDescription=if(visible)"Hide password"else"Show password",tint=Gold)}}}else null)
}
@Composable fun Fact(name:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(name,color=Color(0xFFAEB6CD),fontSize=13.sp,modifier=Modifier.weight(1f));Text(value,color=Color(0xFFF2F0FC),fontWeight=FontWeight.Medium,fontSize=13.sp,modifier=Modifier.weight(1.1f))}}
@Composable fun PageTitle(title:String,subtitle:String="POWERED BY B5 STUDIOS"){Column(Modifier.fillMaxWidth().padding(vertical=15.dp)){Text(subtitle,color=Gold,fontSize=10.sp,letterSpacing=2.sp);Spacer(Modifier.height(5.dp));Text(title,color=Color(0xFFF4F1FF),fontSize=27.sp,fontWeight=FontWeight.Bold)}}
@Composable fun PowerButton(state:String,onClick:()->Unit){
 val connected=state=="Connected";val transition=rememberInfiniteTransition(label="power");val pulse by transition.animateFloat(.7f,1f,infiniteRepeatable(tween(1700),RepeatMode.Reverse),label="power glow");val angle by transition.animateFloat(0f,360f,infiniteRepeatable(tween(11000,easing=LinearEasing)),label="power orbit");val c by animateColorAsState(if(connected)Color(0xFF4EFF9A)else if(state=="Error")Color(0xFFFF8192)else Violet,label="status color")
 val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val press by animateFloatAsState(if(pressed).95f else 1f,label="power depth")
 Box(Modifier.padding(vertical=8.dp).size(234.dp).graphicsLayer{scaleX=press;scaleY=press}.semantics{contentDescription="VPN power: $state"}.clickable(interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center){
  Canvas(Modifier.fillMaxSize()){drawCircle(Brush.radialGradient(listOf(c.copy(alpha=.26f*pulse),Color.Transparent)),size.minDimension/2);val inset=size.width*.09f;rotate(angle){drawArc(Brush.sweepGradient(listOf(Color.Transparent,c.copy(alpha=.8f),Color.Transparent)),0f,235f,false,Offset(inset,inset),Size(size.width-2*inset,size.height-2*inset),style=Stroke(2.2f))};drawCircle(c.copy(alpha=.17f),size.width*.36f,style=Stroke(13f));drawCircle(c.copy(alpha=.7f),size.width*.36f,style=Stroke(2f))}
  Box(Modifier.size(154.dp).shadow(if(pressed)8.dp else 24.dp,CircleShape,ambientColor=c.copy(alpha=.4f),spotColor=c.copy(alpha=.5f)).clip(CircleShape).background(Brush.radialGradient(listOf(c.copy(alpha=.23f),Color(0xFF121426),Color(0xFF080C1B)),center=Offset(70f,20f),radius=420f)).border(2.dp,Brush.verticalGradient(listOf(c,Color(0xFF211838),c.copy(alpha=.6f))),CircleShape),contentAlignment=Alignment.Center){
   Canvas(Modifier.matchParentSize()){drawArc(Color.White.copy(alpha=.13f),210f,120f,false,Offset(size.width*.12f,size.height*.1f),Size(size.width*.76f,size.height*.76f),style=Stroke(3f))}
   Icon(Icons.Outlined.PowerSettingsNew,contentDescription=null,tint=c,modifier=Modifier.size(58.dp))
  }
 }
}
@Composable fun PremiumNav(selected:Int,admin:Boolean=false,onSelect:(Int)->Unit){
 val labels=if(admin)listOf("Home","Licenses","VPN","App","Social")else listOf("Home","Activity","License","Settings")
 val icons=if(admin)listOf(Icons.Outlined.Dashboard,Icons.Outlined.VpnKey,Icons.Outlined.Dns,Icons.Outlined.Settings,Icons.Outlined.Share)else listOf(Icons.Outlined.Home,Icons.Outlined.BarChart,Icons.Outlined.VpnKey,Icons.Outlined.Settings)
 val accent=LocalAccent.current
 Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xF0141229),Color(0xFF070D1C))),RoundedCornerShape(topStart=25.dp,topEnd=25.dp)).border(1.dp,Violet.copy(alpha=.25f),RoundedCornerShape(topStart=25.dp,topEnd=25.dp)).navigationBarsPadding().padding(horizontal=6.dp,vertical=10.dp),horizontalArrangement=Arrangement.SpaceAround){labels.forEachIndexed{i,label->
  val active=i==selected;val scale by animateFloatAsState(if(active)1.07f else 1f,label="navigation")
  Column(Modifier.weight(1f).graphicsLayer{scaleX=scale;scaleY=scale}.clip(RoundedCornerShape(14.dp)).background(if(active)Brush.verticalGradient(listOf(accent.copy(alpha=.22f),Color.Transparent))else Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent))).selectableCompat(active){onSelect(i)}.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icons[i],label,tint=if(active)accent else Color(0xFF9BA7C6),modifier=Modifier.size(24.dp));Spacer(Modifier.height(5.dp));Text(label,color=if(active)Color.White else Color(0xFFAEB6CD),fontSize=10.sp,fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)}
 }}
}
private fun Modifier.selectableCompat(value:Boolean,onClick:()->Unit)=this.semantics{selected=value}.clickable(role=Role.Tab,onClick=onClick)

@Composable fun CustomSections(items:List<Map<String,Any?>>,target:String,open:(String)->Unit){
 items.filter{it["enabled"]==true&&it.str("target")==target}.forEach { item->
  Glass{Row(verticalAlignment=Alignment.CenterVertically){val icon=when(item.str("icon")){"support"->Icons.Outlined.SupportAgent;"shield"->Icons.Outlined.VerifiedUser;"globe"->Icons.Outlined.Public;"message"->Icons.Outlined.ChatBubbleOutline;else->Icons.Outlined.Info};Icon(icon,null,tint=Cyan,modifier=Modifier.size(22.dp));Spacer(Modifier.width(10.dp));Text(item.str("title"),color=Gold,fontWeight=FontWeight.Bold)};if(item.str("body").isNotBlank()){Spacer(Modifier.height(10.dp));Text(item.str("body"),color=Color(0xFFDAD9EB),fontSize=13.sp)};if(item.str("url").isNotBlank())Action("OPEN LINK"){open(item.str("url"))}}
 }
}

@Composable fun CyberConnectionControl(state:String,onClick:()->Unit){
 val connected=state=="Connected";val moving=state in listOf("Connecting","Reconnecting","Disconnecting")
 val transition=rememberInfiniteTransition(label="cyber shield")
 val rotation by transition.animateFloat(0f,360f,infiniteRepeatable(tween(if(moving)4500 else 18000,easing=LinearEasing)),label="security orbit")
 val pulse by transition.animateFloat(.65f,1f,infiniteRepeatable(tween(1500),RepeatMode.Reverse),label="security pulse")
 val accent by animateColorAsState(if(connected)Color(0xFF44FFA2)else if(state=="Error")Color(0xFFFF8096)else Violet,label="security state")
 val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState();val scale by animateFloatAsState(if(pressed).95f else 1f,label="shield press")
 Box(Modifier.padding(vertical=14.dp).size(256.dp).graphicsLayer{scaleX=scale;scaleY=scale}.semantics{contentDescription="VPN connection: $state"}.clickable(interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center){
  Canvas(Modifier.fillMaxSize()){
   val w=size.width;val h=size.height
   drawCircle(Brush.radialGradient(listOf(accent.copy(alpha=.27f*pulse),Color.Transparent)),w*.5f)
   drawOval(Color.Black.copy(alpha=.6f),Offset(w*.16f,h*.79f),Size(w*.68f,h*.12f))
   drawCircle(Color(0xFF151B30),w*.395f,Offset(w*.5f,h*.52f));drawCircle(accent.copy(alpha=.22f),w*.4f,Offset(w*.5f,h*.5f),style=Stroke(1.3.dp.toPx()))
   rotate(rotation){for(i in 0..2)drawArc(Brush.sweepGradient(listOf(Color.Transparent,accent,Color.Transparent)),i*120f,62f,false,Offset(w*.085f,h*.085f),Size(w*.83f,h*.83f),style=Stroke(if(connected)3.dp.toPx() else 1.5.dp.toPx()))}
   for(i in 0..23){val a=i*PI/12;val r=w*.465f;drawLine(accent.copy(alpha=if(i%3==0).65f else .2f),Offset(w*.5f+cos(a).toFloat()*r,h*.5f+sin(a).toFloat()*r),Offset(w*.5f+cos(a).toFloat()*(r-7),h*.5f+sin(a).toFloat()*(r-7)),1.7f)}
  }
  Image(painterResource(R.drawable.usa_shield_art),contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.size(182.dp).clip(CircleShape).graphicsLayer{rotationX=if(pressed)4f else 0f;rotationY=if(pressed)-4f else 0f})
  Box(Modifier.align(Alignment.BottomCenter).padding(bottom=7.dp).shadow(8.dp,RoundedCornerShape(9.dp)).background(Brush.verticalGradient(listOf(Color(0xFF283047),Color(0xFF11182B))),RoundedCornerShape(9.dp)).border(1.dp,accent.copy(alpha=.5f),RoundedCornerShape(9.dp)).padding(horizontal=13.dp,vertical=6.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.PowerSettingsNew,null,tint=accent,modifier=Modifier.size(13.dp));Spacer(Modifier.width(6.dp));Text(if(connected)"DISCONNECT"else if(state=="Disconnecting")"DISCONNECTING"else if(moving)"CANCEL CONNECTION"else"CONNECT",fontSize=9.sp,color=accent,letterSpacing=1.sp)}
  }
 }
}
@Composable fun AnimatedSecureBoot(){
 val transition=rememberInfiniteTransition(label="initialization art");val angle by transition.animateFloat(0f,360f,infiniteRepeatable(tween(6500,easing=LinearEasing)),label="initialization orbit")
 Box(Modifier.size(230.dp),contentAlignment=Alignment.Center){
  Canvas(Modifier.fillMaxSize()){val w=size.width;drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha=.3f),Color.Transparent)));rotate(angle){drawArc(Cyan.copy(alpha=.7f),0f,120f,false,Offset(w*.07f,w*.07f),Size(w*.86f,w*.86f),style=Stroke(2f));drawArc(Violet.copy(alpha=.7f),180f,90f,false,Offset(w*.02f,w*.02f),Size(w*.96f,w*.96f),style=Stroke(3f))}}
  Image(painterResource(R.drawable.usa_shield_art),null,modifier=Modifier.size(155.dp).clip(CircleShape))
 }
 Text("INITIALIZING SECURE SERVICES",fontSize=9.sp,color=Cyan,letterSpacing=2.sp)
}

private fun actionIcon(label:String):ImageVector {
 val text=label.uppercase()
 return when {
  "BAN" in text||"REVOKE" in text->Icons.Outlined.Block
  "DELETE" in text||"REMOVE" in text->Icons.Outlined.DeleteOutline
  "COPY" in text->Icons.Outlined.ContentCopy
  "REACTIVATE" in text->Icons.Outlined.CheckCircleOutline
  "DEVICE" in text->Icons.Outlined.Devices
  "FILE" in text||"OVPN" in text->if("ADD" in text||"IMPORT" in text)Icons.Outlined.UploadFile else Icons.Outlined.FolderOpen
  "GENERATE" in text||"LICENSE" in text->Icons.Outlined.VpnKey
  "SAVE" in text->Icons.Outlined.Save
  "PUBLISH" in text->Icons.Outlined.CloudUpload
  "LOCATION" in text->Icons.Outlined.Public
  "SERVER" in text->Icons.Outlined.Dns
  "SOCIAL" in text||"LINK" in text->Icons.Outlined.Link
  "REFRESH" in text->Icons.Outlined.Refresh
  "SIGN IN" in text->Icons.Outlined.Login
  "PRIVACY" in text||"TERMS" in text->Icons.Outlined.Policy
  "TIPS" in text->Icons.Outlined.Lightbulb
  "ADD" in text||"CREATE" in text->Icons.Outlined.AddCircleOutline
  "SETTINGS" in text||"CONFIGURATION" in text->Icons.Outlined.Settings
  "SECTION" in text||"DASHBOARD" in text->Icons.Outlined.DashboardCustomize
  "VIEW" in text->Icons.Outlined.Visibility
  else->Icons.Outlined.OpenInNew
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun IconAction(label:String,enabled:Boolean=true,onClick:()->Unit){
 TooltipBox(positionProvider=TooltipDefaults.rememberPlainTooltipPositionProvider(),tooltip={PlainTooltip{Text(label)}},state=rememberTooltipState()){
  IconButton(onClick=onClick,enabled=enabled){Icon(actionIcon(label),contentDescription=label,tint=if(enabled)LocalAccent.current else Color.Gray,modifier=Modifier.size(23.dp))}
 }
}

@Composable fun SocialBrandIcon(name:String){val resource=when(name){"whatsapp"->R.drawable.social_whatsapp;"telegram"->R.drawable.social_telegram;"instagram"->R.drawable.social_instagram;"facebook"->R.drawable.social_facebook;"youtube"->R.drawable.social_youtube;"tiktok"->R.drawable.social_tiktok;else->0};if(resource!=0)Icon(painterResource(resource),contentDescription=null,tint=Color.Unspecified,modifier=Modifier.size(23.dp))else Icon(when(name){"email"->Icons.Outlined.Email;"website"->Icons.Outlined.Public;else->Icons.Outlined.Link},contentDescription=null,tint=Cyan,modifier=Modifier.size(23.dp))}
