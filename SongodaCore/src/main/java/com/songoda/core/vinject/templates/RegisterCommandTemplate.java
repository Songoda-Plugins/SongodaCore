package com.songoda.core.vinject.templates;


import net.vortexdevelopment.vinject.annotation.template.RegisterTemplate;

@RegisterTemplate(
        annotationFqcn = "me.ceze88.songodacore.vinject.annotation.RegisterCommand",
        resource = "RegisterCommandTemplate.java.ft",
        name = "Minecraft Listener"
)
public class RegisterCommandTemplate {
}
