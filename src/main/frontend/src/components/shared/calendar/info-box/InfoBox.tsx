import React, { useEffect, useRef, useState } from "react";
import styles from "./InfoBox.module.scss";
import { getNthParent } from "util/domUtils";
import { motion, AnimatePresence } from "framer-motion";

interface Props {
   children: JSX.Element | JSX.Element[];
   className?: string;
}

export const InfoBox: React.FC<Props> = ({ children, className }) => {
   const boxElement = useRef(null);

   const [boxStyle, setBoxStyle] = useState<{
      left?: number | string;
      right?: number | string;
      visibility?: "visible";
   }>({});

   useEffect(() => {
      if (boxElement.current) {
         const column = getNthParent(boxElement.current, 3);
         const table = getNthParent(column, 4);

         table.offsetWidth - column.offsetLeft < 300
            ? setBoxStyle({ right: "110%", visibility: "visible" })
            : setBoxStyle({ left: "110%", visibility: "visible" });
      }
   }, [boxElement]);

   return (
      <AnimatePresence>
         <motion.div
            className={`${styles.info_box_wrapper} ${className ? className : ""}`}
            ref={boxElement}
            style={boxStyle}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
         >
            {children}
         </motion.div>
      </AnimatePresence>
   );
};
