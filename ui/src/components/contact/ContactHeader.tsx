import React from "react";

const ContactHeader = () => {
  return (
    <div className="text-center mb-16">
      <span className="inline-block px-3 py-1 rounded-full bg-savantly/10 text-savantly text-sm font-medium mb-4">
        Contact Us
      </span>
      <h2 className="text-3xl sm:text-4xl md:text-5xl font-light mb-6">
        We&apos;d Love to <span className="text-gradient">Hear from You</span>
      </h2>
      <p className="text-foreground/70 max-w-2xl mx-auto">
        Have a project in mind or questions about our services? Our team is
        ready to discuss how we can help you achieve your technology goals.
      </p>
    </div>
  );
};

export default ContactHeader;
