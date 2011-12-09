/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Alexandre Pinto and Hugo Miranda.
 * Contributor(s): See Appia web page for a list of contributors.
 */
 package gctestfinal;

import net.sf.appia.core.*;

/*
 * Change Log:
 * Nuno Carvalho: 7/Aug/2002 - Alterei o protocolo para nao usar
 *                        codigo deprecated (asynEvent)
 */

public class RainLayer extends Layer {
  
  public RainLayer() {
    
    Class rse=net.sf.appia.protocols.common.RegisterSocketEvent.class;
    Class ecast=RainCastEvent.class;
    Class debug=net.sf.appia.core.events.channel.Debug.class;
    Class timer=RainTimer.class;
    Class init=net.sf.appia.protocols.group.events.GroupInit.class;
    Class esend=RainSendEvent.class;
    Class async=RainAsyncStringEvent.class;
    Class afsync=RainAsyncFrameEvent.class;
    Class view=net.sf.appia.protocols.group.intra.View.class;
    Class cinit=net.sf.appia.core.events.channel.ChannelInit.class;
    Class cclose=net.sf.appia.core.events.channel.ChannelClose.class;
    Class blockok=net.sf.appia.protocols.group.sync.BlockOk.class;
    Class leave=net.sf.appia.protocols.group.leave.LeaveEvent.class;
    Class exit=net.sf.appia.protocols.group.leave.ExitEvent.class;
    Class multicast=net.sf.appia.protocols.udpsimple.MulticastInitEvent.class;
    
    evProvide=new Class[9];
    evProvide[0]=rse;
    evProvide[1]=init;
    evProvide[2]=debug;
    evProvide[3]=timer;
    evProvide[4]=ecast;
    evProvide[5]=esend;
    evProvide[6]=leave;
    evProvide[7]=multicast;
    evProvide[8]=net.sf.appia.protocols.sslcomplete.SslRegisterSocketEvent.class;
    
    
    evRequire=new Class[1];
    evRequire[0]=view;
    
    evAccept=new Class[12];
    evAccept[0]=ecast;
    evAccept[1]=esend;
    evAccept[2]=async;
    evAccept[3]=afsync;
    evAccept[4]=cinit;
    evAccept[5]=cclose;
    evAccept[6]=debug;
    evAccept[7]=timer;
    evAccept[8]=view;
    evAccept[9]=blockok;
    evAccept[10]=exit;
    evAccept[11]=rse;
  }
  public Session createSession() {
    return new RainSession(this);
  }
}
