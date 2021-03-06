import React from "react";
import styles from "./Card.module.scss";

interface Props {
   id?: string;
   className?: string;
   children: JSX.Element | JSX.Element[];
}

export const Card: React.FC<Props> = ({ id, children, className }) => {
   return (
      <div id={id} className={`${styles.card_wrapper} ${className ? className : ""}`}>
         {children}
      </div>
   );
};
