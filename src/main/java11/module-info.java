module m2tk.assistant
{
    requires java.desktop;
    requires java.sql;

    requires transitive m2tk.multiplex;
    requires transitive ch.qos.logback.classic;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires lombok;
    requires cn.hutool.core;
    requires com.miglayout.swing;
    requires org.bytedeco.ffmpeg;
    requires org.bytedeco.javacv;
    requires com.formdev.flatlaf;
    requires com.zaxxer.hikari;
    requires com.h2database;
    requires org.jdbi.v3.core;
    requires com.google.common;
    requires org.jfree.jfreechart;
    requires guru.nidi.graphviz;
}