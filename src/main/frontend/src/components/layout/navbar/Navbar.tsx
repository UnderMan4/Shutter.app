/* eslint-disable jsx-a11y/no-static-element-interactions */
/* eslint-disable jsx-a11y/click-events-have-key-events */
import React, { useState } from "react";
import styles from "./Navbar.module.scss";
import { Link, useLocation } from "react-router-dom";
import {
   ClientButtonList,
   GuestButtonList,
   AdministratorButtonList,
   ModeratorButtonList,
   PhotographerButtonList,
} from "components/layout/button-list";
import { BarButton } from "components/layout";
import { RiLogoutBoxRLine, RiLoginBoxLine } from "react-icons/ri";
import { MdKeyboardArrowRight } from "react-icons/md";
import { AccessLevel } from "types/AccessLevel";
import { useAppSelector } from "redux/hooks";
import { useTranslation } from "react-i18next";
import { LogoutModal } from "../logout-modal/LogoutModal";

export const Navbar = () => {
   const { t } = useTranslation();

   const location = useLocation();
   const path = location.pathname;

   const [expanded, setExpanded] = useState(false);
   const [logoutModalOpen, setLogoutModalOpen] = useState(false);

   const { accessLevel = AccessLevel.GUEST } = useAppSelector((state) => state.auth);

   let btn_list;
   if (accessLevel) {
      switch (accessLevel) {
         case AccessLevel.ADMINISTRATOR:
            btn_list = <AdministratorButtonList path={path} expanded={expanded} />;
            break;
         case AccessLevel.MODERATOR:
            btn_list = <ModeratorButtonList path={path} expanded={expanded} />;
            break;
         case AccessLevel.PHOTOGRAPHER:
            btn_list = <PhotographerButtonList path={path} expanded={expanded} />;
            break;
         case AccessLevel.CLIENT:
            btn_list = <ClientButtonList path={path} expanded={expanded} />;
            break;
         default:
            btn_list = <GuestButtonList path={path} expanded={expanded} />;
            break;
      }
   }

   return (
      <div
         className={`${styles.layout_bar_navbar} ${
            expanded ? styles.expanded : ""
         } ${accessLevel.toLowerCase()}`}
      >
         <div className={styles.logo_container}>
            <Link to="/" className={styles.logo_wrapper}>
               <img
                  src="/icons/logo.svg"
                  alt="shutter logo"
                  className={styles.navbar_logo}
               />
               <p className={`section-title ${expanded ? "" : styles.hide_text}`}>
                  {t("navbar.title")}
               </p>
            </Link>
         </div>
         <div className={styles.bar_button_list_wrapper}>
            <div className={styles.bar_button_wrapper}>{btn_list}</div>
            <div className={styles.bar_button_wrapper}>
               {accessLevel === AccessLevel.GUEST ? (
                  <BarButton
                     to="login"
                     icon={<RiLoginBoxLine />}
                     text={t("navbar.buttons.login")}
                     expanded={expanded}
                     active={path === "/login"}
                  />
               ) : (
                  <BarButton
                     onClick={() => setLogoutModalOpen(true)}
                     icon={<RiLogoutBoxRLine />}
                     text={t("navbar.buttons.logout")}
                     expanded={expanded}
                  />
               )}
            </div>
         </div>
         <div
            className={styles.expand_button_wrapper}
            onClick={() => setExpanded(!expanded)}
         >
            <MdKeyboardArrowRight />
         </div>

         <LogoutModal isOpen={logoutModalOpen} setIsOpen={setLogoutModalOpen} />
      </div>
   );
};
