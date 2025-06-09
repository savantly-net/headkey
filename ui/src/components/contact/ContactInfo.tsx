import React from "react";
import { Phone, Mail, MapPin } from "lucide-react";

interface ContactItemProps {
  icon: React.ReactNode;
  title: string;
  value: string;
  delay?: string;
}

const ContactItem: React.FC<ContactItemProps> = ({
  icon,
  title,
  value,
  delay = "0s",
}) => {
  return (
    <div
      className="glass p-6 animate-fade-in"
      style={{ animationDelay: delay }}
    >
      <div className="flex items-start">
        <div className="flex-shrink-0 w-12 h-12 rounded-full bg-savantly/10 flex items-center justify-center text-savantly mr-4">
          {icon}
        </div>
        <div>
          <h3 className="text-xl font-normal mb-2">{title}</h3>
          <p className="text-foreground/70">{value}</p>
        </div>
      </div>
    </div>
  );
};

const ContactInfo = () => {
  return (
    <div className="space-y-8">
      <ContactItem
        icon={<Phone className="w-5 h-5" />}
        title="Phone"
        value="+1 (682) 231-9369"
      />

      <ContactItem
        icon={<Mail className="w-5 h-5" />}
        title="Email"
        value="support@savantly.net"
        delay="0.1s"
      />

      <ContactItem
        icon={<MapPin className="w-5 h-5" />}
        title="Location"
        value="Fort Worth, TX, USA"
        delay="0.2s"
      />
    </div>
  );
};

export default ContactInfo;
