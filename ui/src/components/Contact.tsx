import React from "react";
import ContactHeader from "./contact/ContactHeader";
import ContactInfo from "./contact/ContactInfo";
import ContactForm from "./contact/ContactForm";

const Contact = () => {
  return (
    <section
      id="contact"
      className="section-padding bg-gradient-to-b from-background to-muted"
    >
      <div className="container mx-auto">
        <ContactHeader />

        <div className="grid grid-cols-1 lg:grid-cols-5 gap-12 max-w-6xl mx-auto">
          <div className="lg:col-span-2">
            <ContactInfo />
          </div>

          <div
            className="lg:col-span-3 glass p-8 animate-fade-in"
            style={{ animationDelay: "0.3s" }}
          >
            <ContactForm />
          </div>
        </div>
      </div>
    </section>
  );
};

export default Contact;
