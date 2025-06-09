"use client";

import React, { useState } from "react";
import { toast } from "sonner";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";

const ContactForm = () => {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    message: "",
  });

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const response = await fetch(
        "https://cloud.activepieces.com/api/v1/webhooks/lnUp5Z3yQELDrtokw0Afy",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            firstName: formData.name,
            email: formData.email,
            message: formData.message,
          }),
        },
      );

      if (response.ok) {
        toast.success("Message sent successfully!", {
          description: "We will get back to you shortly.",
        });

        setFormData({
          name: "",
          email: "",
          message: "",
        });
      } else {
        toast.error("Failed to send message", {
          description: "Please try again later.",
        });
      }
    } catch (error) {
      console.error("Error sending message:", error);
      toast.error("Failed to send message", {
        description: "Please try again later.",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <div>
          <label
            htmlFor="name"
            className="block text-sm font-medium text-foreground/80 mb-2"
          >
            Name
          </label>
          <Input
            id="name"
            name="name"
            type="text"
            value={formData.name}
            onChange={handleChange}
            className="input-field"
            required
          />
        </div>

        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-foreground/80 mb-2"
          >
            Email
          </label>
          <Input
            id="email"
            name="email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            className="input-field"
            required
          />
        </div>
      </div>

      <div>
        <label
          htmlFor="message"
          className="block text-sm font-medium text-foreground/80 mb-2"
        >
          Message
        </label>
        <Textarea
          id="message"
          name="message"
          rows={5}
          value={formData.message}
          onChange={handleChange}
          className="input-field resize-none"
          required
        />
      </div>

      <Button
        type="submit"
        disabled={isSubmitting}
        className="btn-primary w-full flex items-center justify-center"
      >
        {isSubmitting ? (
          <span className="flex items-center">
            <Loader2 className="animate-spin mr-2 h-4 w-4" />
            Sending...
          </span>
        ) : (
          "Send Message"
        )}
      </Button>
    </form>
  );
};

export default ContactForm;
