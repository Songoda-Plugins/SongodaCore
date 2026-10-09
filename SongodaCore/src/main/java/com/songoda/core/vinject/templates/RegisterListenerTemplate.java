package com.songoda.core.vinject.templates;


import net.vortexdevelopment.vinject.annotation.template.RegisterTemplate;

@RegisterTemplate(
        annotationFqcn = "com.songoda.core.vinject.annotation.RegisterListener",
        resource = "RegisterListenerTemplate.java.ft",
        name = "Minecraft Listener"
)
public class RegisterListenerTemplate {
}
