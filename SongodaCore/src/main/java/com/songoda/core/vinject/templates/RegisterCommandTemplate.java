package com.songoda.core.vinject.templates;


import net.vortexdevelopment.vinject.annotation.template.RegisterTemplate;

@RegisterTemplate(
        annotationFqcn = "com.songoda.core.vinject.annotation.RegisterCommand",
        resource = "RegisterCommandTemplate.java.ft",
        name = "Minecraft BaseCommand"
)
public class RegisterCommandTemplate {
}
